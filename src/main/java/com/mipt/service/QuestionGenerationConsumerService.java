package com.mipt.service;

import com.mipt.QuestionGenerator;
import com.mipt.dbAPI.DatabaseAccessException;
import com.mipt.dbAPI.DbService;
import com.mipt.domainModel.Game;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;

@Service
public class QuestionGenerationConsumerService {

  private static final Logger LOGGER = LoggerFactory.getLogger(QuestionGenerationConsumerService.class);

  private final DbService dbService;

  public QuestionGenerationConsumerService(DbService dbService) {
    this.dbService = dbService;
  }

  /**
   * Читает из Kafka запросы на генерацию вопросов ML, запускает генерацию через LLM,
   * затем сохраняет сгенерированные вопросы в базу данных.
   */
  @KafkaListener(topics = "${app.kafka.topic.ml-question-requests}", groupId = "${spring.kafka.consumer.group-id}")
  public void consumeQuestionGenerationRequest(String message) {
    try {
      Game game = fromJson(message);
      String topicName = dbService.getTopicById(game.getTopicId()).getName();
      int difficult = game.getLevelDifficultyInt();
      int gameId = game.getGameId();
      int numberOfQuestions = game.getNumberOfQuestions();

      String generatorPayload = buildGeneratorPayload(topicName, numberOfQuestions, difficult);
      LOGGER.info("Processing question generation request for gameId={}, topicId={}", gameId, game.getTopicId());

      CompletableFuture<String> future = QuestionGenerator.generate(generatorPayload);
      future
          .thenAccept(rawJson -> persistQuestions(gameId, rawJson))
          .exceptionally(ex -> {
            LOGGER.error("Failed to generate questions for gameId={}", gameId, ex);
            return null;
          })
          .join();
    } catch (Exception e) {
      LOGGER.error("Failed to process Kafka message: {}", message, e);
    }
  }

  private static Game fromJson(String message) {
    JSONObject json = new JSONObject(message);
    Game game = new Game();
    game.setGameId(json.getInt("gameId"));
    game.setTopicId(json.getInt("topicId"));
    game.setNumberOfQuestions(json.getInt("numberOfQuestions"));
    game.setLevelDifficulty(json.getInt("levelDifficulty"));
    return game;
  }

  private void persistQuestions(int gameId, String rawJson) {
    if (rawJson == null || rawJson.isBlank()) {
      LOGGER.warn("Empty or null result from generator for gameId={}", gameId);
      return;
    }
    try {
      JSONArray questionsJson = new JSONArray(rawJson);
      dbService.loadQuestions(gameId, questionsJson);
      LOGGER.info("Persisted {} questions for gameId={}", questionsJson.length(), gameId);
    } catch (DatabaseAccessException | SQLException e) {
      LOGGER.error("Failed to load generated questions into DB for gameId={}", gameId, e);
    } catch (Exception e) {
      LOGGER.error("Failed to parse generated questions for gameId={}", gameId, e);
    }
  }

  private static String buildGeneratorPayload(String topicName, int numberOfQuestions, int levelDifficulty) {
    return "[{\"topic\":\"" + topicName + "\",\"numberOfQuestions\":" + numberOfQuestions
        + ",\"difficult\":" + levelDifficulty + "}]";
  }
}
