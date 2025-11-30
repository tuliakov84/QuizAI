package com.mipt.domainModel;

import java.time.Instant;

public class AnswerObject extends Question {
  private String session;
  private int timeTakenToAnswerInSeconds;
  private Game.LevelDifficulty levelDifficulty;

  public String getSession() {
    return session;
  }

  public void setSession(String session) {
    this.session = session;
  }

  public int getTimeTakenToAnswerInSeconds() {
    return timeTakenToAnswerInSeconds;
  }

  public void setTimeTakenToAnswerInSeconds(int timeTakenToAnswerInSeconds) {
    this.timeTakenToAnswerInSeconds = timeTakenToAnswerInSeconds;
  }

  public Game.LevelDifficulty getLevelDifficulty() {
    return levelDifficulty;
  }

  public void setLevelDifficulty(Game.LevelDifficulty levelDifficulty) {
    this.levelDifficulty = levelDifficulty;
  }
}
