package com.mipt.domainModel;

import java.time.Instant;

import com.mipt.gameModes.GameMode;

public class CurrentGameObject {
  private Integer gameId;
  private Instant gameStartTime;
  private GameMode gameMode;


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

  public GameMode getGameMode() {
    return gameMode;
  }

  public void setGameMode(GameMode gameMode) {
    this.gameMode = gameMode;
  }
}