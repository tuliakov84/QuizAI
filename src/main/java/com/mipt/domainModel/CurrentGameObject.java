package com.mipt.domainModel;

import java.time.Instant;

public class CurrentGameObject {
  private Integer gameId;
  private Instant gameStartTime;


  public Integer getGameId() {
    return gameId;
  }

  public void setGameId(Integer gameId) {
    this.gameId = gameId;
  }

  public Instant getGameStartTime() {
    return gameStartTime;
  }

  public void setGameStartTime(Instant gameStartTime) {
    this.gameStartTime = gameStartTime;
  }
}
