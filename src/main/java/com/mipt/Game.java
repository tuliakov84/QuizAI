package com.mipt;

import java.util.Date;

public class Game {
  int gameId;
  int authorId;
  int topicId;

  byte status;
  Date gameStartTime;
  Date gameEndTime;
  boolean isPrivate;
  int difficulty;
  int numberOfParticipants;

  int currentQuestionId;

  String question;
  String[] answerOptions;
  int rightAnswerIndex;
  int points;
}
