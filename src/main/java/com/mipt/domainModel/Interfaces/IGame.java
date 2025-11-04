package com.mipt.domainModel.Interfaces;

import com.mipt.domainModel.*;

import java.time.Instant;

public interface IGame {
  int getGameId();
  void setGameId(int gameId);

  User getAuthor();
  void setAuthor(User author);

  Topic getTopic();
  void setTopic(Topic topic);

  Question getQuestion();
  void setQuestion(Question question);

  Game.Status getStatus();
  void setStatus(Game.Status status);

  Instant getCreatedAt();
  void setCreatedAt(Instant createdAt);

  Instant getGameStartTime();
  void setGameStartTime(Instant gameStartTime);

  Instant getGameEndTime();
  void setGameEndTime(Instant gameEndTime);

  int getCurrentQuestionNumber();
  void setCurrentQuestionNumber(int currentQuestionNumber);

  int getNumberOfQuestions();
  void setNumberOfQuestions(int numberOfQuestions);

  boolean isPrivate();
  void setPrivate(boolean isPrivate);

  Game.LevelDifficulty getLevelDifficulty();
  void setLevelDifficulty(Game.LevelDifficulty levelDifficulty);

  int getParticipantsNumber();
  void setParticipantsNumber(int participantsNumber);
}
