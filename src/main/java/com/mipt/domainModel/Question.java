package com.mipt.domainModel;

public class Question {
  private Integer questionId;
  private Integer gameId;

  private String questionText;
  private String answer1;
  private String answer2;
  private String answer3;
  private String answer4;
  private Integer rightAnswerNumber;
  private Integer questionNumber;
  private Integer submittedAnswerNumber;

  public Question(){

  }

  public Question(Integer questionId, Integer gameId, String questionText,
                  String answer1, String answer2, String answer3, String answer4,
                  Integer rightAnswerNumber, Integer questionNumber) {
    this.questionId = questionId;
    this.gameId = gameId;
    this.questionText = questionText;
    this.answer1 = answer1;
    this.answer2 = answer2;
    this.answer3 = answer3;
    this.answer4 = answer4;
    this.rightAnswerNumber = rightAnswerNumber;
    this.questionNumber = questionNumber;
  }

  public Integer getQuestionId() {
    return questionId;
  }

  public void setQuestionId(Integer questionId) {
    this.questionId = questionId;
  }

  public Integer getGameId() {
    return gameId;
  }

  public void setGameId(Integer gameId) {
    this.gameId = gameId;
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

  public Integer getRightAnswerNumber() {
    return rightAnswerNumber;
  }

  public void setRightAnswerNumber(Integer rightAnswerNumber) {
    this.rightAnswerNumber = rightAnswerNumber;
  }

  public Integer getQuestionNumber() {
    return questionNumber;
  }

  public void setQuestionNumber(Integer questionNumber) {
    this.questionNumber = questionNumber;
  }

  public Integer getSubmittedAnswerNumber() {
    return submittedAnswerNumber;
  }

  public void setSubmittedAnswerNumber(Integer submittedAnswerNumber) {
    this.submittedAnswerNumber = submittedAnswerNumber;
  }
}
