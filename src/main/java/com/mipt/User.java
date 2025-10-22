package com.mipt;

import java.sql.Time;
import java.util.Date;

public class User {
  int userId;
  int currentGameId;
  int[] achievementsId;
  int[] gamesPlayedId;

  String username;
  String email;
  String password;
  String pic_path;
  String status;
  Date last_activity;
  int gamesPlayedNumber;
  int globalPoints;
  int globalPossiblePoints;
  int currentGamePoints;
}
