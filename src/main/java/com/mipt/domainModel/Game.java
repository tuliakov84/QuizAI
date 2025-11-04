package com.mipt.domainModel;

import com.mipt.domainModel.Interfaces.IGame;

import java.time.Instant;

public class Game implements IGame {

  private int gameId;
  private User author;
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

  @Override
  public int getGameId() {
    return gameId;
  }

  @Override
  public void setGameId(int gameId) {
    this.gameId = gameId;
  }

  @Override
  public User getAuthor() {
    return author;
  }

  @Override
  public void setAuthor(User author) {
    this.author = author;
  }

  @Override
  public Topic getTopic() {
    return topic;
  }

  @Override
  public void setTopic(Topic topic) {
    this.topic = topic;
  }

  @Override
  public Question getQuestion() {
    return question;
  }

  @Override
  public void setQuestion(Question question) {
    this.question = question;
  }

  @Override
  public Status getStatus() {
    return status;
  }

  @Override
  public void setStatus(Status status) {
    this.status = status;
  }

  @Override
  public Instant getCreatedAt() {
    return createdAt;
  }

  @Override
  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  @Override
  public Instant getGameStartTime() {
    return gameStartTime;
  }

  @Override
  public void setGameStartTime(Instant gameStartTime) {
    this.gameStartTime = gameStartTime;
  }

  @Override
  public Instant getGameEndTime() {
    return gameEndTime;
  }

  @Override
  public void setGameEndTime(Instant gameEndTime) {
    this.gameEndTime = gameEndTime;
  }

  @Override
  public int getCurrentQuestionNumber() {
    return currentQuestionNumber;
  }

  @Override
  public void setCurrentQuestionNumber(int currentQuestionNumber) {
    this.currentQuestionNumber = currentQuestionNumber;
  }

  @Override
  public int getNumberOfQuestions() {
    return numberOfQuestions;
  }

  @Override
  public void setNumberOfQuestions(int numberOfQuestions) {
    this.numberOfQuestions = numberOfQuestions;
  }

  @Override
  public boolean isPrivate() {
    return isPrivate;
  }

  @Override
  public void setPrivate(boolean isPrivate) {
    this.isPrivate = isPrivate;
  }

  @Override
  public LevelDifficulty getLevelDifficulty() {
    return levelDifficulty;
  }

  @Override
  public void setLevelDifficulty(LevelDifficulty levelDifficulty) {
    this.levelDifficulty = levelDifficulty;
  }

  @Override
  public int getParticipantsNumber() {
    return participantsNumber;
  }

  @Override
  public void setParticipantsNumber(int participantsNumber) {
    this.participantsNumber = participantsNumber;
  }
}
