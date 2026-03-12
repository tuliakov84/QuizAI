package com.mipt.dbAPI.jpa.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "answered_correctly_questions")
public class AnsweredCorrectlyQuestionEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Integer id;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "user_id", nullable = false)
  private UserEntity user;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "question_id", nullable = false)
  private QuestionEntity question;

  public Integer getId() {
    return id;
  }

  public UserEntity getUser() {
    return user;
  }

  public void setUser(UserEntity user) {
    this.user = user;
  }

  public QuestionEntity getQuestion() {
    return question;
  }

  public void setQuestion(QuestionEntity question) {
    this.question = question;
  }
}
