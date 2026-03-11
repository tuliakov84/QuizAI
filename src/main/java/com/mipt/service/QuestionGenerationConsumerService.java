package com.mipt.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mipt.QuestionGenerator;
import com.mipt.dbAPI.DatabaseAccessException;
import com.mipt.dbAPI.DbService;
import com.mipt.domainModel.Game;
import org.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.sql.SQLException;

/**
 * Consumes question generation requests from Kafka (Game payload), calls the LLM to generate
 * questions, and persists the results to the database.
 */
@Service
public class QuestionGenerationConsumerService {

  private static final Logger LOGGER = LoggerFactory.getLogger(QuestionGenerationConsumerService.class);

  private final DbService dbService;
  private final ObjectMapper objectMapper;

  public QuestionGenerationConsumerService(DbService dbService, ObjectMapper objectMapper) {
    this.dbService = dbService;
    this.objectMapper = objectMapper;
  }

  @KafkaListener(topics = "${app.kafka.topic.question-generation-requests}", groupId = "${spring.kafka.consumer.group-id}")
  public void consumeQuestionGenerationRequest(String message) {
    try {
      Game game = objectMapper.readValue(message, Game.class);
      LOGGER.info("Consumed question generation request for gameId={} topicId={}",
          game.getGameId(), game.getTopicId());

      String topicName = dbService.getTopicById(game.getTopicId()).getName();
      int levelDifficultyInt = levelDifficultyToInt(game.getLevelDifficulty());

      String payload = buildGeneratorPayload(
          topicName,
          game.getNumberOfQuestions(),
          levelDifficultyInt);

      QuestionGenerator.generate(payload)
          .exceptionally(ex -> {
            LOGGER.error("Failed to generate questions for gameId={}", game.getGameId(), ex);
            return null;
          })
          .thenAccept(json -> {
            if (json != null) {
              persistQuestions(game.getGameId(), json);
            }
          })
          .join();
    } catch (Exception e) {
      LOGGER.error("Failed to process question generation message: {}", message, e);
    }
  }

  private static int levelDifficultyToInt(Game.LevelDifficulty level) {
    if (level == null) return 1;
    return switch (level) {
      case EASY -> 1;
      case MEDIUM -> 2;
      case HARD -> 3;
    };
  }

  private void persistQuestions(int gameId, String rawJson) {
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
