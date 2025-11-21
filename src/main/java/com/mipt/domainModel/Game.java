package com.mipt.domainModel;

import java.time.Instant;

public class Game {

  private int gameId;
  private int authorId;
  private Topic topic;
  private Question question;
  private Status status;
  private Instant createdAt;
  private Instant gameStartTime;
  private Instant gameEndTime;
  private int currentQuestionNumber;
  private int numberOfQuestions;
  private boolean isPrivate;
  private LevelDifficulty levelDifficulty;
  private int participantsNumber;
  //private int currentParticipantsNumber;

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

  public Game(int gameId, int authorId, Topic topic, LevelDifficulty levelDifficulty) {
    this.gameId = gameId;
    this.authorId = authorId;
    this.topic = topic;
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

  public Topic getTopic() {
    return topic;
  }

  public void setTopic(Topic topic) {
    this.topic = topic;
  }

  public Question getQuestion() {
    return question;
  }

  public void setQuestion(Question question) {
    this.question = question;
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

  public void setLevelDifficulty(LevelDifficulty levelDifficulty) {
    this.levelDifficulty = levelDifficulty;
  }

  public int getParticipantsNumber() {
    return participantsNumber;
  }

  public void setParticipantsNumber(int participantsNumber) {
    this.participantsNumber = participantsNumber;
  }
}
