package com.mipt.domainModel;

public class DuelActionRequest {
  private String session;
  private Integer gameId;
  private String action;
  private Integer amount;
  private Integer questionNumber;
  private Integer submittedAnswerNumber;
  private String submittedAnswerText;

  public String getSession() {
    return session;
  }

  public void setSession(String session) {
    this.session = session;
  }

  public Integer getGameId() {
    return gameId;
  }

  public void setGameId(Integer gameId) {
    this.gameId = gameId;
  }

  public String getAction() {
    return action;
  }

  public void setAction(String action) {
    this.action = action;
  }

  public Integer getAmount() {
    return amount;
  }

  public void setAmount(Integer amount) {
    this.amount = amount;
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

  public String getSubmittedAnswerText() {
    return submittedAnswerText;
  }

  public void setSubmittedAnswerText(String submittedAnswerText) {
    this.submittedAnswerText = submittedAnswerText;
  }
}

