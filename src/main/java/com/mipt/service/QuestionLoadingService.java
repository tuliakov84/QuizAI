package com.mipt.service;

import com.mipt.QuestionGenerator;
import com.mipt.dbAPI.DatabaseAccessException;
import com.mipt.dbAPI.DbService;
import com.mipt.domainModel.Topic;
import org.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.sql.SQLException;

@Service
public class QuestionLoadingService {

  private static final Logger LOGGER = LoggerFactory.getLogger(QuestionLoadingService.class);

  private final DbService dbService;

  public QuestionLoadingService(DbService dbService) {
    this.dbService = dbService;
  }

  /**
   * Triggers question generation for the provided game without blocking the caller thread.
   */
  @Async
  public void loadQuestionsAsync(int gameId, int levelDifficulty, int numberOfQuestions, int topicId) {
    try {
      Topic topic = dbService.getTopicById(topicId);
      String payload = buildGeneratorPayload(topic.getName(), numberOfQuestions, levelDifficulty);

      QuestionGenerator.generate(payload)
          .thenAccept(json -> persistQuestions(gameId, json))
          .exceptionally(ex -> {
            LOGGER.error("Failed to generate questions for game {}", gameId, ex);
            return null;
          });
    } catch (DatabaseAccessException | SQLException e) {
      LOGGER.error("Failed to start question generation for game {}", gameId, e);
    }
  }

  private void persistQuestions(int gameId, String rawJson) {
    try {
      JSONArray questionsJson = new JSONArray(rawJson);
      dbService.loadQuestions(gameId, questionsJson);
    } catch (DatabaseAccessException | SQLException e) {
      LOGGER.error("Failed to load generated questions into DB for game {}", gameId, e);
    } catch (Exception e) {
      LOGGER.error("Failed to parse generated questions for game {}", gameId, e);
    }
  }

  private String buildGeneratorPayload(String topicName, int numberOfQuestions, int levelDifficulty) {
    return "[{\"topic\":\"" + topicName + "\",\"numberOfQuestions\":" + numberOfQuestions
        + ",\"difficult\":" + levelDifficulty + "}]";
  }
}

