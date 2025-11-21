package com.mipt.domainModel;

public class Question {
  private int questionId;
  private Game game;

  private String questionText;
  private String answer1;
  private String answer2;
  private String answer3;
  private String answer4;
  private int rightAnswerNumber;
  private int questionNumber;

  public Question(){

  }

  public Question(int questionId, Game game, String questionText,
                  String answer1, String answer2, String answer3, String answer4,
                  int rightAnswerNumber, int questionNumber) {
    this.questionId = questionId;
    this.game = game;
    this.questionText = questionText;
    this.answer1 = answer1;
    this.answer2 = answer2;
    this.answer3 = answer3;
    this.answer4 = answer4;
    this.rightAnswerNumber = rightAnswerNumber;
    this.questionNumber = questionNumber;
  }

  public int getQuestionId() {
    return questionId;
  }

  public void setQuestionId(int questionId) {
    this.questionId = questionId;
  }

  public Game getGame() {
    return game;
  }

  public void setGame(Game game) {
    this.game = game;
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

  public int getQuestionNumber() {
    return questionNumber;
  }

  public void setQuestionNumber(int questionNumber) {
    this.questionNumber = questionNumber;
  }
}
