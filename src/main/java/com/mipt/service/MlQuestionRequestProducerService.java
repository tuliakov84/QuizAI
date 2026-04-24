package com.mipt.service;

import com.mipt.domainModel.Game;
import com.mipt.gameModes.GameMode;
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
  public void sendQuestionGenerationRequest(Game game, String topicTitle) {
    String payload = toJson(game, topicTitle, UUID.randomUUID().toString(), 0, null, null, null);
    int gameId = game.getGameId();
    try {
      LOGGER.info(
          "Step P1: sending initial generation request. gameId={}, topicId={}, difficulty={}, numberOfQuestions={}",
          gameId, game.getTopicId(), game.getLevelDifficultyInt(), game.getNumberOfQuestions()
      );
      kafkaTemplate.send(this.topicName, String.valueOf(gameId), payload);
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
      String topicTitle,
      GameMode gameMode,
      String requestId,
      int attempt,
      String questionNumbersToRegenerateJsonArray,
      String questionIdsToReplaceJsonArray,
      String existingQuestionsJsonArray
  ) {
    String payload = toJson(
        buildGame(gameId, topicId, levelDifficulty, numberOfQuestions, gameMode),
        topicTitle,
        requestId,
        attempt,
        questionNumbersToRegenerateJsonArray,
        questionIdsToReplaceJsonArray,
        existingQuestionsJsonArray
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

  private static Game buildGame(int gameId, int topicId, int levelDifficulty, int numberOfQuestions, GameMode gameMode) {
    Game game = new Game();
    game.setGameId(gameId);
    game.setTopicId(topicId);
    game.setLevelDifficulty(levelDifficulty);
    game.setNumberOfQuestions(numberOfQuestions);
    game.setGameMode(gameMode);
    return game;
  }

  private static String toJson(
      Game game,
      String topicName,
      String requestId,
      int attempt,
      String questionNumbersToRegenerateJsonArray,
      String questionIdsToReplaceJsonArray,
      String existingQuestionsJsonArray
  ) {
    JSONObject obj = new JSONObject();
    obj.put("requestId", requestId);
    obj.put("status", "REQUESTED");
    obj.put("gameId", game.getGameId());
    obj.put("topicId", game.getTopicId());
    obj.put("topicName", topicName);
    obj.put("numberOfQuestions", game.getNumberOfQuestions());
    obj.put("levelDifficulty", game.getLevelDifficultyInt());
    obj.put("gameMode", game.getGameMode() == null ? GameMode.CASUAL.name() : game.getGameMode().name());
    obj.put("attempt", attempt);
    if (questionNumbersToRegenerateJsonArray != null && !questionNumbersToRegenerateJsonArray.isBlank()) {
      obj.put("questionNumbersToRegenerate", new org.json.JSONArray(questionNumbersToRegenerateJsonArray));
      if (questionIdsToReplaceJsonArray != null && !questionIdsToReplaceJsonArray.isBlank()) {
        obj.put("questionIdsToReplace", new org.json.JSONArray(questionIdsToReplaceJsonArray));
      }
      if (existingQuestionsJsonArray != null && !existingQuestionsJsonArray.isBlank()) {
        obj.put("existingQuestions", new org.json.JSONArray(existingQuestionsJsonArray));
      }
      obj.put("isRegeneration", true);
    } else {
      obj.put("isRegeneration", false);
    }
    return obj.toString();
  }
}
