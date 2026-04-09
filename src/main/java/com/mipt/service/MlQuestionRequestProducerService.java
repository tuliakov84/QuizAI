package com.mipt.service;

import com.mipt.domainModel.Game;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class MlQuestionRequestProducerService {

  private static final Logger LOGGER = LoggerFactory.getLogger(MlQuestionRequestProducerService.class);

  private final KafkaTemplate<String, String> kafkaTemplate;

  @Value("${app.kafka.topic.question-generation-requests}")
  private String topicName;

  public MlQuestionRequestProducerService(KafkaTemplate<String, String> kafkaTemplate) {
    this.kafkaTemplate = kafkaTemplate;
  }

  /**
   * Отправляет в Kafka сообщение с данными игры для генерации вопросов LLM.
   * Консьюмер использует эти данные для вызова ML-сервиса и сохранения результатов в БД.
   */
  public void sendQuestionGenerationRequest(Game game) {
    String payload = toJson(game, UUID.randomUUID().toString(), 0, null, null);
    int gameId = game.getGameId();
    try {
      LOGGER.info(
          "Step P1: sending initial generation request. gameId={}, topicId={}, difficulty={}, numberOfQuestions={}",
          gameId, game.getTopicId(), game.getLevelDifficultyInt(), game.getNumberOfQuestions()
      );
      kafkaTemplate.send(topicName, String.valueOf(gameId), payload);
      LOGGER.info("Sent question generation request for gameId={}, topicId={}", gameId, game.getTopicId());
    } catch (Exception e) {
      LOGGER.error("Failed to send question generation request for gameId={}", gameId, e);
      throw new RuntimeException("Failed to send question generation request to Kafka", e);
    }
  }

  public void sendRegenerationRequest(
      int gameId,
      int topicId,
      int levelDifficulty,
      int numberOfQuestions,
      String requestId,
      int attempt,
      String questionNumbersToRegenerateJsonArray,
      String questionIdsToReplaceJsonArray
  ) {
    String payload = toJson(
        buildGame(gameId, topicId, levelDifficulty, numberOfQuestions),
        requestId,
        attempt,
        questionNumbersToRegenerateJsonArray,
        questionIdsToReplaceJsonArray
    );
    try {
      LOGGER.info(
          "Step P2: sending regeneration request. gameId={}, requestId={}, attempt={}, questionNumbers={}, questionIdsToReplace={}",
          gameId, requestId, attempt, questionNumbersToRegenerateJsonArray, questionIdsToReplaceJsonArray
      );
      kafkaTemplate.send(topicName, String.valueOf(gameId), payload);
      LOGGER.info("Sent regeneration request for gameId={}, requestId={}, attempt={}", gameId, requestId, attempt);
    } catch (Exception e) {
      LOGGER.error("Failed to send regeneration request for gameId={}, requestId={}", gameId, requestId, e);
      throw new RuntimeException("Failed to send regeneration request to Kafka", e);
    }
  }

  private static Game buildGame(int gameId, int topicId, int levelDifficulty, int numberOfQuestions) {
    Game game = new Game();
    game.setGameId(gameId);
    game.setTopicId(topicId);
    game.setLevelDifficulty(levelDifficulty);
    game.setNumberOfQuestions(numberOfQuestions);
    return game;
  }

  private static String toJson(
      Game game,
      String requestId,
      int attempt,
      String questionNumbersToRegenerateJsonArray,
      String questionIdsToReplaceJsonArray
  ) {
    JSONObject obj = new JSONObject();
    obj.put("requestId", requestId);
    obj.put("status", "REQUESTED");
    obj.put("gameId", game.getGameId());
    obj.put("topicId", game.getTopicId());
    obj.put("numberOfQuestions", game.getNumberOfQuestions());
    obj.put("levelDifficulty", game.getLevelDifficultyInt());
    obj.put("attempt", attempt);
    if (questionNumbersToRegenerateJsonArray != null && !questionNumbersToRegenerateJsonArray.isBlank()) {
      obj.put("questionNumbersToRegenerate", new org.json.JSONArray(questionNumbersToRegenerateJsonArray));
      if (questionIdsToReplaceJsonArray != null && !questionIdsToReplaceJsonArray.isBlank()) {
        obj.put("questionIdsToReplace", new org.json.JSONArray(questionIdsToReplaceJsonArray));
      }
      obj.put("isRegeneration", true);
    } else {
      obj.put("isRegeneration", false);
    }
    return obj.toString();
  }
}
