package com.mipt.domainModel;

import com.mipt.gameModes.GameMode;

import java.util.List;

public class LobbyObject {
  private Integer gameId;
  private Status status;
  private List<String> playersUsernames;
  private List<LobbyPlayer> players;
  private Boolean questionReady;
  private Integer participantsNumber;
  private Integer currentParticipantsNumber;
  private GameMode gameMode;

  public Boolean getReady() {
    return questionReady;
  }

  public void setReady(Boolean readyVal) {
    questionReady = readyVal;
  }

  public Boolean getQuestionsReady() {
    return questionReady;
  }

  public void setQuestionsReady(Boolean questionsReady) {
    this.questionReady = questionsReady;
  }

  private enum Status {
    WAITING,
    IN_PROGRESS,
    ON_PAUSE,
    ENDED
  }

  public Integer getGameId() {
    return gameId;
  }

  public void setGameId(Integer gameId) {
    this.gameId = gameId;
  }
  
  public List<String> getPlayersUsernames() {
    return playersUsernames;
  }

  public void setPlayersUsernames(List<String> playersUsernames) {
    this.playersUsernames = playersUsernames;
  }

  public List<LobbyPlayer> getPlayers() {
    return players;
  }

  public void setPlayers(List<LobbyPlayer> players) {
    this.players = players;
  }

  public Integer getParticipantsNumber() {
    return participantsNumber;
  }

  public void setParticipantsNumber(Integer participantsNumber) {
    this.participantsNumber = participantsNumber;
  }

  public Integer getCurrentParticipantsNumber() {
    return currentParticipantsNumber;
  }

  public void setCurrentParticipantsNumber(Integer currentParticipantsNumber) {
    this.currentParticipantsNumber = currentParticipantsNumber;
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

  public GameMode getGameMode() {
    return gameMode;
  }

  public void setGameMode(GameMode gameMode) {
    this.gameMode = gameMode;
  }
}
