package com.mipt.domainModel;

import java.time.Instant;

public class Game {
  private int gameId;
  private int authorId;
  private int topicId;
  private int questionId;
  private Status status;
  private Instant createdAt;
  private Instant gameStartTime;
  private Instant gameEndTime;
  private int currentQuestionNumber;
  private int numberOfQuestions;
  private boolean isPrivate;
  private LevelDifficulty levelDifficulty;
  private int participantsNumber;
  private int currentParticipantsNumber;

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

  public Game(int gameId, int authorId, int topicId, LevelDifficulty levelDifficulty) {
    this.gameId = gameId;
    this.authorId = authorId;
    this.topicId = topicId;
    this.levelDifficulty = levelDifficulty;
    this.status = Status.WAITING;
    this.createdAt = Instant.now();
  }

  public int getGameId() {
    return gameId;
  }

  public void setGameId(int gameId) {
    this.gameId = gameId;
  }

  public int getAuthorId() {
    return authorId;
  }

  public void setAuthorId(int authorId) {
    this.authorId = authorId;
  }

  public int getTopicId() {
    return topicId;
  }

  public void setTopicId(int topicId) {
    this.topicId = topicId;
  }

  public int getQuestionId() {
    return questionId;
  }

  public void setQuestionId(int questionId) {
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

  public int getCurrentQuestionNumber() {
    return currentQuestionNumber;
  }

  public void setCurrentQuestionNumber(int currentQuestionNumber) {
    this.currentQuestionNumber = currentQuestionNumber;
  }

  public int getNumberOfQuestions() {
    return numberOfQuestions;
  }

  public void setNumberOfQuestions(int numberOfQuestions) {
    this.numberOfQuestions = numberOfQuestions;
  }

  public boolean isPrivate() {
    return isPrivate;
  }

  public void setPrivate(boolean isPrivate) {
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

  public int getParticipantsNumber() {
    return participantsNumber;
  }

  public void setParticipantsNumber(int participantsNumber) {
    this.participantsNumber = participantsNumber;
  }

  public int getCurrentParticipantsNumber() {
    return currentParticipantsNumber;
  }

  public void setCurrentParticipantsNumber(int currentParticipantsNumber) {
    this.currentParticipantsNumber = currentParticipantsNumber;
  }
}
