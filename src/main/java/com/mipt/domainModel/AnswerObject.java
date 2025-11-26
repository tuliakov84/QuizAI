package com.mipt.domainModel;

import java.time.Instant;

public class AnswerObject extends Question {
  private String session;
  private Instant timeTakenToAnswer;
  private Game.LevelDifficulty levelDifficulty;

  public String getSession() {
    return session;
  }

  public void setSession(String session) {
    this.session = session;
  }

  public Instant getTimeTakenToAnswer() {
    return timeTakenToAnswer;
  }

  public void setTimeTakenToAnswer(Instant timeTakenToAnswer) {
    this.timeTakenToAnswer = timeTakenToAnswer;
  }

  public Game.LevelDifficulty getLevelDifficulty() {
    return levelDifficulty;
  }

  public void setLevelDifficulty(Game.LevelDifficulty levelDifficulty) {
    this.levelDifficulty = levelDifficulty;
  }
}
