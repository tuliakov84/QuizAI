package com.mipt.domainModel;

import java.time.Instant;

public class User {
  public int userId;
  public Game currentGame;
  public Achievement[] achievements;
  public Game[] gamesPlayed;

  public String session;
  public String username;
  public String password;
  public int picId;
  public String description;
  public Instant lastActivity;
  public int gamesPlayedNumber;
  public int globalPoints;
  public int globalPossiblePoints;
  public int currentGamePoints;

  public User(int userId, String password, String username) {
    this.userId = userId;
    this.password = password;
    this.username = username;
  }

  private void updateLastActivity() {
    this.lastActivity = Instant.now();
  }

  public String getSession() {
    return session;
  }

  public void setSession(String session) {
    this.session = session;

  }

  public int getUserId() {
    return 0;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String newUsername) {
    this.username = newUsername;

  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;

  }

  public int getPicId() {
    return picId;
  }

  public void setPicId(int picId) {
    this.picId = picId;

  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;

  }

  public Instant getLastActivity() {
    return lastActivity;
  }

  public void setLastActivity(Instant lastActivity) {
    this.lastActivity = lastActivity;
  }

  public int getGamesPlayedNumber() {
    return gamesPlayedNumber;
  }

  public void setGamesPlayedNumber(int gamesPlayedNumber) {
    this.gamesPlayedNumber = gamesPlayedNumber;

  }

  public int getGlobalPoints() {
    return globalPoints;
  }

  public void setGlobalPoints(int globalPoints) {
    this.globalPoints = globalPoints;

  }

  public int getGlobalPossiblePoints() {
    return globalPossiblePoints;
  }

  public void setGlobalPossiblePoints(int globalPossiblePoints) {
    this.globalPossiblePoints = globalPossiblePoints;

  }

  public int getCurrentGamePoints() {
    return currentGamePoints;
  }

  public void setCurrentGamePoints(int currentGamePoints) {
    this.currentGamePoints = currentGamePoints;

  }

  public Game getCurrentGame() {
    return currentGame;
  }

  public void setCurrentGame(Game currentGame) {
    this.currentGame = currentGame;
  }

  public Achievement[] getAchievements() {
    return achievements;
  }

  public void setAchievements(Achievement[] achievements) {
    this.achievements = achievements;
  }

  public Game[] getGamesPlayed() {
    return gamesPlayed;
  }

  public void setGamesPlayed(Game[] gamesPlayed) {
    this.gamesPlayed = gamesPlayed;
  }
}
