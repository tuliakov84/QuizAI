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
  private Integer status;

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
  private Integer participantsNumber;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "topic_id")
  private TopicEntity topic;

  @Column(name = "game_start_time")
  private Timestamp gameStartTime;

  @Column(name = "game_end_time")
  private Timestamp gameEndTime;

  /**
   * After async Python semantic validation completes successfully ({@code VALIDATED} event).
   * Until then the lobby must not report the game as ready even if questions exist in DB.
   */
  @Column(name = "questions_validated")
  private Boolean questionsValidated;

  public Integer getId() {
    return id;
  }

  public Integer getStatus() {
    return status;
  }

  public void setStatus(Integer status) {
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

  public Integer getParticipantsNumber() {
    return participantsNumber;
  }

  public void setParticipantsNumber(Integer participantsNumber) {
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

  public Boolean getQuestionsValidated() {
    return questionsValidated;
  }

  public void setQuestionsValidated(Boolean questionsValidated) {
    this.questionsValidated = questionsValidated;
  }
}
