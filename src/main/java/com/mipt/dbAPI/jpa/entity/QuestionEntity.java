package com.mipt.dbAPI.jpa.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "questions")
public class QuestionEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Integer id;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "game_id", nullable = false)
  private GameEntity game;

  @Column(name = "question_number")
  private int questionNumber;

  @Column(name = "question_text")
  private String questionText;

  @Column(name = "answer1")
  private String answer1;

  @Column(name = "answer2")
  private String answer2;

  @Column(name = "answer3")
  private String answer3;

  @Column(name = "answer4")
  private String answer4;

  @Column(name = "right_answer_number")
  private int rightAnswerNumber;

  public Integer getId() {
    return id;
  }

  public GameEntity getGame() {
    return game;
  }

  public void setGame(GameEntity game) {
    this.game = game;
  }

  public int getQuestionNumber() {
    return questionNumber;
  }

  public void setQuestionNumber(int questionNumber) {
    this.questionNumber = questionNumber;
  }

  public String getQuestionText() {
    return questionText;
  }

  public void setQuestionText(String questionText) {
    this.questionText = questionText;
  }

  public String getAnswer1() {
    return answer1;
  }

  public void setAnswer1(String answer1) {
    this.answer1 = answer1;
  }

  public String getAnswer2() {
    return answer2;
  }

  public void setAnswer2(String answer2) {
    this.answer2 = answer2;
  }

  public String getAnswer3() {
    return answer3;
  }

  public void setAnswer3(String answer3) {
    this.answer3 = answer3;
  }

  public String getAnswer4() {
    return answer4;
  }

  public void setAnswer4(String answer4) {
    this.answer4 = answer4;
  }

  public int getRightAnswerNumber() {
    return rightAnswerNumber;
  }

  public void setRightAnswerNumber(int rightAnswerNumber) {
    this.rightAnswerNumber = rightAnswerNumber;
  }
}
