package com.mipt;

import java.util.Date;

public class Game {
  int gameId;
  int authorId;
  int topicId;
  int[] questionsIds;

  byte status;
  Date gameStartTime;
  Date gameEndTime;
  boolean isPrivate;
  int difficulty;
  int numberOfParticipants;
}
