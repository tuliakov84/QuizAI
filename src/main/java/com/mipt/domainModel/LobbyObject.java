package com.mipt.domainModel;

import java.util.List;

public class LobbyObject {
  private int gameId;
  private Status status;
  private List<String> playersUsernames;
  private Boolean questionReady;

  public Boolean getReady() {
    return questionReady;
  }

  public void setReady(Boolean readyVal) {
    questionReady = readyVal;
  }

  private enum Status {
    WAITING,
    IN_PROGRESS,
    ON_PAUSE,
    ENDED
  }

  public int getGameId() {
    return gameId;
  }

  public void setGameId(int gameId) {
    this.gameId = gameId;
  }
  
  public List<String> getPlayersUsernames() {
    return playersUsernames;
  }

  public void setPlayersUsernames(List<String> playersUsernames) {
    this.playersUsernames = playersUsernames;
  }

  public void setStatus(Integer status) {
    switch (status) {
      case 0 -> this.status = Status.WAITING;
      case 1 -> this.status = Status.ON_PAUSE;
      case 2 -> this.status = Status.IN_PROGRESS;
      case 3 -> this.status = Status.ENDED;
    }
  }

  public Status getStatus() {
    return status;
  }
}
