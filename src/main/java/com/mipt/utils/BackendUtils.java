package com.mipt.utils;

import com.mipt.dbAPI.DatabaseAccessException;
import com.mipt.domainModel.Game;

public class BackendUtils {

  private static final double MAX_TIME_TO_ANSWER_IN_SECONDS = 20;

  public int countPoints(int levelDifficulty, int timeTakenToAnswerInSeconds) {
    int difficultMultiplier = 100 * levelDifficulty;
    double timeMultiplier = (0.5 + 0.5 * (1 - (timeTakenToAnswerInSeconds / MAX_TIME_TO_ANSWER_IN_SECONDS)));

    return (int) (difficultMultiplier * timeMultiplier);
  }

  public int countPossiblePoints(int levelDifficulty) {
    int difficultMultiplier = 100  * levelDifficulty;

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
