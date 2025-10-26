package com.mipt.domainModel.Interfaces;

import com.mipt.domainModel.Achievement;
import com.mipt.domainModel.Game;

import java.time.Instant;

public interface IUser {
  int getUserId();
  String getUsername();
  void setUsername(String username);

  String getPassword();
  void setPassword(String password);

  String getSession();
  void setSession(String session);

  int getPicId();
  void setPicId(int picId);

  String getDescription();
  void setDescription(String description);

  Instant getLastActivity();
  void setLastActivity(Instant lastActivity);

  int getGamesPlayedNumber();
  void setGamesPlayedNumber(int gamesPlayedNumber);

  int getGlobalPoints();
  void setGlobalPoints(int globalPoints);

  int getGlobalPossiblePoints();
  void setGlobalPossiblePoints(int globalPossiblePoints);

  int getCurrentGamePoints();
  void setCurrentGamePoints(int currentGamePoints);

  Game getCurrentGame();
  void setCurrentGame(Game currentGame);

  Achievement[] getAchievements();
  void setAchievements(Achievement[] achievements);

  Game[] getGamesPlayed();
  void setGamesPlayed(Game[] gamesPlayed);
}
