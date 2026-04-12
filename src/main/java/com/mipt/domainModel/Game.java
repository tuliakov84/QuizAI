package com.mipt.domainModel;

import com.mipt.gameModes.GameMode;

import java.time.Instant;

public class Game {
  private Integer gameId;
  private Integer authorId;
  private Integer topicId;
  private Integer questionId;
  private Status status;
  private Instant createdAt;
  private Instant gameStartTime;
  private Instant gameEndTime;
  private Integer numberOfQuestions;
  private boolean isPrivate;
  private LevelDifficulty levelDifficulty;
  private Integer participantsNumber;
  private Integer currentParticipantsNumber;
  private GameMode gameMode;

  public enum Status {
    WAITING,
    IN_PROGRESS,
    ON_PAUSE,
    ENDED
  }

  public enum LevelDifficulty {
    EASY,
    MEDIUM,
    HARD
  }

  public Game() {
  }

  public Game(Integer gameId, Integer authorId, Integer topicId, LevelDifficulty levelDifficulty) {
    this.gameId = gameId;
    this.authorId = authorId;
    this.topicId = topicId;
    this.levelDifficulty = levelDifficulty;
    this.status = Status.WAITING;
    this.createdAt = Instant.now();
  }

  public Integer getGameId() {
    return gameId;
  }

  public void setGameId(Integer gameId) {
    this.gameId = gameId;
  }

  public Integer getAuthorId() {
    return authorId;
  }

  public void setAuthorId(Integer authorId) {
    this.authorId = authorId;
  }

  public Integer getTopicId() {
    return topicId;
  }

  public void setTopicId(Integer topicId) {
    this.topicId = topicId;
  }

  public Integer getQuestionId() {
    return questionId;
  }

  public void setQuestionId(Integer questionId) {
    this.questionId = questionId;
  }

  public Status getStatus() {
    return status;
  }

  public void setStatus(Status status) {
    this.status = status;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public Instant getGameStartTime() {
    return gameStartTime;
  }

  public void setGameStartTime(Instant gameStartTime) {
    this.gameStartTime = gameStartTime;
  }

  public Instant getGameEndTime() {
    return gameEndTime;
  }

  public void setGameEndTime(Instant gameEndTime) {
    this.gameEndTime = gameEndTime;
  }

  public Integer getNumberOfQuestions() {
    return numberOfQuestions;
  }

  public void setNumberOfQuestions(Integer numberOfQuestions) {
    this.numberOfQuestions = numberOfQuestions;
  }

  public boolean getIsPrivate() {
    return isPrivate;
  }

  public void setIsPrivate(boolean isPrivate) {
    this.isPrivate = isPrivate;
  }

  public LevelDifficulty getLevelDifficulty() {
    return levelDifficulty;
  }

  public void setLevelDifficulty(Integer levelDifficulty) {
    switch (levelDifficulty) {
      case 1 -> this.levelDifficulty = LevelDifficulty.EASY;
      case 2 -> this.levelDifficulty = LevelDifficulty.MEDIUM;
      case 3 -> this.levelDifficulty = LevelDifficulty.HARD;
    }
  }

  /**
   * Возвращает числовую сложность для payload LLM/ML: 1 = EASY, 2 = MEDIUM, 3 = HARD.
   */
  public int getLevelDifficultyInt() {
    if (levelDifficulty == null) {
      return 1;
    }
    return switch (levelDifficulty) {
      case EASY -> 1;
      case MEDIUM -> 2;
      case HARD -> 3;
    };
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

  public GameMode getGameMode() {
    return gameMode;
  }

  public void setGameMode(GameMode gameMode) {
    this.gameMode = gameMode;
  }
}
