package com.mipt.service;

import com.mipt.dbAPI.DatabaseAccessException;
import com.mipt.dbAPI.DbService;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.List;

@Service
@ConditionalOnProperty(name = "app.service.role", havingValue = "backend", matchIfMissing = true)
public class QuestionGenerationConsumerService {

  private static final Logger LOGGER = LoggerFactory.getLogger(QuestionGenerationConsumerService.class);

  private final DbService dbService;
  private final MlQuestionRequestProducerService producerService;

  @Value("${app.kafka.message.max-regeneration-attempts:3}")
  private int maxRegenerationAttempts;

  public QuestionGenerationConsumerService(DbService dbService, MlQuestionRequestProducerService producerService) {
    this.dbService = dbService;
    this.producerService = producerService;
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

      switch (status) {
        case "VALIDATED" -> handleValidated(gameId, payload);
        case "REGENERATE" -> handleRegeneration(payload);
        case "GENERATED" -> LOGGER.debug("Generated event received for gameId={} - waiting for validator", gameId);
        default -> LOGGER.debug("Skip result event with unsupported status='{}' for gameId={}", status, gameId);
      }
    } catch (Exception e) {
      LOGGER.error("Failed to process Kafka result message: {}", message, e);
    }
  }

  private void handleValidated(int gameId, JSONObject payload) {
    JSONArray questions = payload.optJSONArray("questions");
    if (questions == null) {
      questions = payload.optJSONArray("selected_questions");
    }
    if (questions == null || questions.isEmpty()) {
      LOGGER.warn("Validated event has no questions for gameId={}", gameId);
      return;
    }

    try {
      List<Integer> questionIds = dbService.loadQuestions(gameId, questions);
      LOGGER.info("Persisted {} validated questions for gameId={}", questionIds.size(), gameId);
    } catch (DatabaseAccessException | SQLException e) {
      LOGGER.error("Failed to persist validated questions for gameId={}", gameId, e);
    }
  }

  private void handleRegeneration(JSONObject payload) {
    int gameId = payload.getInt("gameId");
    int topicId = payload.has("topicId") ? payload.getInt("topicId") : payload.optInt("topic_id");
    int levelDifficulty = payload.has("levelDifficulty") ? payload.getInt("levelDifficulty") : payload.optInt("level_difficulty");
    int numberOfQuestions = payload.has("numberOfQuestions") ? payload.getInt("numberOfQuestions") : payload.optInt("number_of_questions");
    int attempt = payload.optInt("attempt", 0);
    String requestId = payload.has("requestId") ? payload.optString("requestId", "") : payload.optString("request_id", "");
    JSONArray questionNumbersToRegenerate = payload.optJSONArray("questionNumbersToRegenerate");
    if (questionNumbersToRegenerate == null) {
      questionNumbersToRegenerate = payload.optJSONArray("question_numbers_to_regenerate");
    }

    if (questionNumbersToRegenerate == null || questionNumbersToRegenerate.isEmpty()) {
      LOGGER.warn("Regeneration event has empty questionNumbersToRegenerate for gameId={}", gameId);
      return;
    }

    if (attempt >= maxRegenerationAttempts) {
      LOGGER.error(
          "Max regeneration attempts reached for gameId={}, requestId={}, attempt={}",
          gameId, requestId, attempt
      );
      return;
    }

    producerService.sendRegenerationRequest(
        gameId,
        topicId,
        levelDifficulty,
        numberOfQuestions,
        requestId.isBlank() ? String.valueOf(gameId) : requestId,
        attempt + 1,
        questionNumbersToRegenerate.toString()
    );
  }
}
