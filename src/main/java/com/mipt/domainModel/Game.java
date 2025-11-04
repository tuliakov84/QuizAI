package com.mipt.domainModel;

import java.time.Instant;

public class Game {

  public int gameId;
  public User author;
  public Topic topic;
  public Question question;
  public Status status;
  public Instant createdAt;
  public Instant gameStartTime;
  public Instant gameEndTime;
  public int currentQuestionNumber;
  public int numberOfQuestions;
  public boolean isPrivate;
  public LevelDifficulty levelDifficulty;
  public int participantsNumber;

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

  public Game(int gameId, User author, Topic topic, LevelDifficulty levelDifficulty) {
    this.gameId = gameId;
    this.author = author;
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

  public User getAuthor() {
    return author;
  }

  public void setAuthor(User author) {
    this.author = author;
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
