package com.mipt.domainModel;

public class AnswerObject extends Question {
  private String session;
  private Integer timeTakenToAnswerInSeconds;
  private Game.LevelDifficulty levelDifficulty;

  public String getSession() {
    return session;
  }

  public void setSession(String session) {
    this.session = session;
  }

  public Integer getTimeTakenToAnswerInSeconds() {
    return timeTakenToAnswerInSeconds;
  }

  public void setTimeTakenToAnswerInSeconds(Integer timeTakenToAnswerInSeconds) {
    this.timeTakenToAnswerInSeconds = timeTakenToAnswerInSeconds;
  }

  public Game.LevelDifficulty getLevelDifficulty() {
    return levelDifficulty;
  }

  public void setLevelDifficulty(Game.LevelDifficulty levelDifficulty) {
    this.levelDifficulty = levelDifficulty;
  }
}
