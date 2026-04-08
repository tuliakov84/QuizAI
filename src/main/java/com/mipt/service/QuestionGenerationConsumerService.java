package com.mipt.service;

import com.mipt.dbAPI.DatabaseAccessException;
import com.mipt.dbAPI.DbService;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
@ConditionalOnExpression(
    "'${app.service.role:backend}' == 'backend' || '${app.service.role:backend}' == 'both'"
)
public class QuestionGenerationConsumerService {

  private static final Logger LOGGER = LoggerFactory.getLogger(QuestionGenerationConsumerService.class);

  private final DbService dbService;
  private final MlQuestionRequestProducerService producerService;
  private final PythonValidationRequestProducerService pythonValidationRequestProducer;
  private final Set<String> processedGeneratedEvents = ConcurrentHashMap.newKeySet();
  private final Set<String> processedRegenerationEvents = ConcurrentHashMap.newKeySet();

  @Value("${app.kafka.message.max-regeneration-attempts:3}")
  private int maxRegenerationAttempts;

  public QuestionGenerationConsumerService(
      DbService dbService,
      MlQuestionRequestProducerService producerService,
      PythonValidationRequestProducerService pythonValidationRequestProducer
  ) {
    this.dbService = dbService;
    this.producerService = producerService;
    this.pythonValidationRequestProducer = pythonValidationRequestProducer;
  }

  /**
   * Читает из Kafka результаты генерации/валидации.
   * - VALIDATED: сохраняет вопросы в БД;
   * - REGENERATE: отправляет запрос на перегенерацию в topic #1.
   */
  @KafkaListener(topics = "${app.kafka.topic.question-generation-results}", groupId = "${spring.kafka.consumer.group-id}")
  public void consumeQuestionGenerationResult(String message) {
    try {
      JSONObject payload = new JSONObject(message);
      String status = payload.optString("status", "").trim().toUpperCase();
      int gameId = payload.getInt("gameId");
      LOGGER.info("Step 1/8: backend received generation result. gameId={}, status={}", gameId, status);

      if ("GENERATED".equals(status)) {
        handleGenerated(gameId, payload);
      } else {
        LOGGER.debug("Skip result event with unsupported status='{}' for gameId={}", status, gameId);
      }
    } catch (Exception e) {
      LOGGER.error("Failed to process Kafka result message: {}", message, e);
    }
  }

  @KafkaListener(topics = "${app.kafka.topic.python-validation-results}", groupId = "${spring.kafka.consumer.group-id}")
  public void consumePythonValidationResult(String message) {
    try {
      JSONObject payload = new JSONObject(message);
      String status = payload.optString("status", "").trim().toUpperCase();
      LOGGER.info(
          "Step 6/8: backend received python validation result. gameId={}, status={}, requestId={}, attempt={}",
          payload.optInt("gameId"),
          status,
          payload.optString("requestId", ""),
          payload.optInt("attempt", 0)
      );
      if ("REGENERATE".equals(status)) {
        handleRegeneration(payload);
      } else if ("VALIDATED".equals(status)) {
        int validatedGameId = payload.getInt("gameId");
        dbService.setQuestionsValidated(validatedGameId, true);
        LOGGER.info(
            "Step 5/8: python validation OK — game marked ready for lobby. gameId={}, requestId={}, attempt={}",
            validatedGameId,
            payload.optString("requestId", ""),
            payload.optInt("attempt", 0)
        );
      } else {
        LOGGER.debug("Skip python validation event with status='{}': {}", status, message);
      }
    } catch (Exception e) {
      LOGGER.error("Failed to process python validation message: {}", message, e);
    }
  }

  private void handleGenerated(int gameId, JSONObject payload) {
    JSONArray questions = payload.optJSONArray("questions");
    if (questions == null || questions.isEmpty()) {
      LOGGER.warn("Generated event has no questions for gameId={}", gameId);
      return;
    }

    String requestId = payload.optString("requestId", String.valueOf(gameId));
    int attempt = payload.optInt("attempt", 0);
    String eventKey = requestId + ":" + attempt + ":" + gameId;
    if (!processedGeneratedEvents.add(eventKey)) {
      LOGGER.info("Skip duplicate GENERATED event for gameId={}, requestId={}, attempt={}", gameId, requestId, attempt);
      return;
    }

    try {
      JSONArray questionIdsToReplaceJson = payload.optJSONArray("questionIdsToReplace");
      List<Integer> persistedQuestionIds;
      if (questionIdsToReplaceJson != null && !questionIdsToReplaceJson.isEmpty()) {
        List<Integer> questionIdsToReplace = toIntList(questionIdsToReplaceJson);
        LOGGER.info(
            "Step 2/8: replacing existing questions in DB. gameId={}, replaceCount={}, requestId={}, attempt={}",
            gameId, questionIdsToReplace.size(), requestId, attempt
        );
        dbService.replaceQuestionsByIds(gameId, questionIdsToReplace, questions);
        persistedQuestionIds = questionIdsToReplace;
      } else {
        LOGGER.info(
            "Step 2/8: storing generated questions in DB. gameId={}, generatedCount={}, requestId={}, attempt={}",
            gameId, questions.length(), requestId, attempt
        );
        persistedQuestionIds = dbService.loadQuestions(gameId, questions);
      }

      LOGGER.info(
          "Step 3/8: DB save finished. gameId={}, persistedQuestionIds={}",
          gameId, persistedQuestionIds
      );
      dbService.setQuestionsValidated(gameId, false);
      LOGGER.info("Step 3.1/8: questions marked not validated until python OK. gameId={}", gameId);
      pythonValidationRequestProducer.sendValidationRequest(gameId, requestId, attempt, persistedQuestionIds);
      LOGGER.info("Step 4/8: sent validation request to python. gameId={}, requestId={}, attempt={}", gameId, requestId, attempt);
    } catch (DatabaseAccessException | SQLException e) {
      LOGGER.error("Failed to persist generated questions for gameId={}", gameId, e);
    }
  }

  private void handleRegeneration(JSONObject payload) {
    int gameId = payload.getInt("gameId");
    int topicId = payload.has("topicId") ? payload.getInt("topicId") : payload.optInt("topic_id");
    int levelDifficulty = payload.has("levelDifficulty") ? payload.getInt("levelDifficulty") : payload.optInt("level_difficulty");
    int numberOfQuestions = payload.has("numberOfQuestions") ? payload.getInt("numberOfQuestions") : payload.optInt("number_of_questions");
    int attempt = payload.optInt("attempt", 0);
    String requestId = payload.has("requestId") ? payload.optString("requestId", "") : payload.optString("request_id", "");
    String eventKey = (requestId.isBlank() ? String.valueOf(gameId) : requestId) + ":" + attempt + ":" + gameId;
    if (!processedRegenerationEvents.add(eventKey)) {
      LOGGER.info("Skip duplicate REGENERATE event for gameId={}, requestId={}, attempt={}", gameId, requestId, attempt);
      return;
    }
    JSONArray questionIdsToRegenerate = payload.optJSONArray("questionIdsToRegenerate");
    if (questionIdsToRegenerate == null) {
      questionIdsToRegenerate = payload.optJSONArray("question_ids_to_regenerate");
    }

    if (questionIdsToRegenerate == null || questionIdsToRegenerate.isEmpty()) {
      LOGGER.warn("Regeneration event has empty questionIdsToRegenerate for gameId={}", gameId);
      return;
    }

    if (attempt >= maxRegenerationAttempts) {
      LOGGER.error(
          "Max regeneration attempts reached for gameId={}, requestId={}, attempt={}",
          gameId, requestId, attempt
      );
      return;
    }

    try {
      if (topicId <= 0 || levelDifficulty <= 0 || numberOfQuestions <= 0) {
        Integer[] preset = dbService.getPreset(gameId);
        if (topicId <= 0) {
          topicId = preset[4];
        }
        if (levelDifficulty <= 0) {
          levelDifficulty = preset[1];
        }
        if (numberOfQuestions <= 0) {
          numberOfQuestions = preset[2];
        }
      }
      List<Integer> questionIds = toIntList(questionIdsToRegenerate);
      JSONArray questionNumbersToRegenerate = new JSONArray(dbService.getQuestionNumbersByIds(gameId, questionIds));
      LOGGER.info(
          "Step 7/8: regeneration needed. gameId={}, requestId={}, attempt={}, questionIds={}, questionNumbers={}",
          gameId, requestId, attempt, questionIds, questionNumbersToRegenerate
      );

      producerService.sendRegenerationRequest(
          gameId,
          topicId,
          levelDifficulty,
          numberOfQuestions,
          requestId.isBlank() ? String.valueOf(gameId) : requestId,
          attempt + 1,
          questionNumbersToRegenerate.toString(),
          questionIdsToRegenerate.toString()
      );
      LOGGER.info(
          "Step 8/8: sent regeneration request to generator. gameId={}, nextAttempt={}, questionCount={}",
          gameId, attempt + 1, questionIds.size()
      );
    } catch (DatabaseAccessException | SQLException e) {
      LOGGER.error("Failed to map question ids to numbers for gameId={}", gameId, e);
    }
  }

  private static List<Integer> toIntList(JSONArray jsonArray) {
    List<Integer> result = new ArrayList<>();
    for (int i = 0; i < jsonArray.length(); i++) {
      result.add(jsonArray.getInt(i));
    }
    return result;
  }
}
