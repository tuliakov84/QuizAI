package com.mipt.utils;

import com.mipt.dbAPI.DatabaseAccessException;
import com.mipt.dbAPI.DbService;

import com.mipt.domainModel.Game;

import java.sql.SQLException;
import java.time.Instant;
import java.util.List;

public class BackendUtils {

  private static final double MAX_TIME_TO_ANSWER_IN_SECONDS = 20;

  private final DbService dbService;

  public BackendUtils(DbService dbService) {
    this.dbService = dbService;
  }

  public int countPoints(Game.LevelDifficulty levelDifficulty, Instant timeTakenToAnswer) {
    int difficultMultiplier;
    switch (levelDifficulty) {
      case EASY -> difficultMultiplier = 100;
      case MEDIUM -> difficultMultiplier = 200;
      case HARD -> difficultMultiplier = 300;
      default -> difficultMultiplier = 100;
    }
    int timeMultiplier = (int) (0.5 + 0.5 * (1 - (timeTakenToAnswer.getEpochSecond() / MAX_TIME_TO_ANSWER_IN_SECONDS)));

    return difficultMultiplier * timeMultiplier;
  }

  public int countPossiblePoints(Game.LevelDifficulty levelDifficulty) {
    int difficultMultiplier;
    switch (levelDifficulty) {
      case EASY -> difficultMultiplier = 100;
      case MEDIUM -> difficultMultiplier = 200;
      case HARD -> difficultMultiplier = 300;
      default -> difficultMultiplier = 100;
    }

    return difficultMultiplier;
  }

  public boolean isSessionCollision(DatabaseAccessException exception) {
    String message = exception.getMessage();
    return message != null && message.contains("Session key is not unique");
  }

  public String generateSessionId() {
    return java.util.UUID.randomUUID().toString().replace("-", "");
  }
}
