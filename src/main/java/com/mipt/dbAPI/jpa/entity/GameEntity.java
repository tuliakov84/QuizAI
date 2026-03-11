package com.mipt.dbAPI.jpa.entity;

import jakarta.persistence.*;

import java.sql.Timestamp;

@Entity
@Table(name = "games")
public class GameEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Integer id;

  @Column(name = "status")
  private int status;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "author_id")
  private UserEntity author;

  @Column(name = "created_at")
  private Timestamp createdAt;

  @Column(name = "is_private")
  private Boolean isPrivate;

  @Column(name = "level_difficulty")
  private Integer levelDifficulty;

  @Column(name = "number_of_questions")
  private Integer numberOfQuestions;

  @Column(name = "participants_number")
  private int participantsNumber;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "topic_id")
  private TopicEntity topic;

  @Column(name = "game_start_time")
  private Timestamp gameStartTime;

  @Column(name = "game_end_time")
  private Timestamp gameEndTime;

  public Integer getId() {
    return id;
  }

  public int getStatus() {
    return status;
  }

  public void setStatus(int status) {
    this.status = status;
  }

  public UserEntity getAuthor() {
    return author;
  }

  public void setAuthor(UserEntity author) {
    this.author = author;
  }

  public Timestamp getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Timestamp createdAt) {
    this.createdAt = createdAt;
  }

  public Boolean getIsPrivate() {
    return isPrivate;
  }

  public void setPrivate(Boolean aPrivate) {
    isPrivate = aPrivate;
  }

  public Integer getLevelDifficulty() {
    return levelDifficulty;
  }

  public void setLevelDifficulty(Integer levelDifficulty) {
    this.levelDifficulty = levelDifficulty;
  }

  public Integer getNumberOfQuestions() {
    return numberOfQuestions;
  }

  public void setNumberOfQuestions(Integer numberOfQuestions) {
    this.numberOfQuestions = numberOfQuestions;
  }

  public int getParticipantsNumber() {
    return participantsNumber;
  }

  public void setParticipantsNumber(int participantsNumber) {
    this.participantsNumber = participantsNumber;
  }

  public TopicEntity getTopic() {
    return topic;
  }

  public void setTopic(TopicEntity topic) {
    this.topic = topic;
  }

  public Timestamp getGameStartTime() {
    return gameStartTime;
  }

  public void setGameStartTime(Timestamp gameStartTime) {
    this.gameStartTime = gameStartTime;
  }

  public Timestamp getGameEndTime() {
    return gameEndTime;
  }

  public void setGameEndTime(Timestamp gameEndTime) {
    this.gameEndTime = gameEndTime;
  }
}
