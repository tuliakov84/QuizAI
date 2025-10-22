package com.mipt.domainModel;

import java.time.Instant;

public class Game {
  int gameId;
  User author;
  Topic topic;

  enum status {
    WAITING,
    inPROGRESS,
    onPAUSE,
    ENDED
  }

  Instant createdAt;
  Instant gameStartTime;
  Instant gameEndTime;
  int currentQuestionNumber;
  int numberOfQuestions;
  boolean isPrivate;

  enum level_difficulty {
    EASY,
    MEDIUM,
    HARD
  }

  int participantsNumber;
}
