package com.mipt.domainModel.Interfaces;

import com.mipt.domainModel.Game;

public interface IQuestion {
  int getQuestionId();
  void setQuestionId(int questionId);

  Game getGame();
  void setGame(Game game);

  String getQuestionText();
  void setQuestionText(String questionText);

  String getAnswer1();
  void setAnswer1(String answer1);

  String getAnswer2();
  void setAnswer2(String answer2);

  String getAnswer3();
  void setAnswer3(String answer3);

  String getAnswer4();
  void setAnswer4(String answer4);

  int getRightAnswerNumber();
  void setRightAnswerNumber(int rightAnswerNumber);

  int getQuestionNumber();
  void setQuestionNumber(int questionNumber);
}
