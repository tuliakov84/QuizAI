package com.mipt.domainModel;

import java.time.Instant;

public class User {
  int userId;
  Game currentGame;
  Achievement[] achievements;
  Game[] gamesPlayed;

  String session;
  String username;
  String password;
  int picId;
  String description;
  Instant last_activity;
  int gamesPlayedNumber;
  int globalPoints;
  int globalPossiblePoints;
  int currentGamePoints;
}
