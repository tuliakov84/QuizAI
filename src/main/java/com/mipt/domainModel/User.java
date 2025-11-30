package com.mipt.domainModel;

import java.time.Instant;
import java.util.List;

public class User {
  private int userId;
  private Game currentGame;
  private Integer[] achievements;
  private Integer[] gamesPlayed;

  private String session;
  private String username;
  private String password;
  private int picId;
  private String description;
  private Instant lastActivity;
  private int gamesPlayedNumber;
  private int globalPoints;
  private int globalPossiblePoints;
  private int currentGamePoints;

  public User() {

  }

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

  public void setUserId(int userId) {
    this.userId = userId;
  }
  
  public int getUserId() {
    return userId;
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

  public Integer[] getAchievements() {
    return achievements;
  }

  public void setAchievements(Integer[] achievements) {
    this.achievements = achievements;
  }

  public Integer[] getGamesPlayed() {
    return gamesPlayed;
  }

  public void setGamesPlayed(Integer[] gamesPlayed) {
    this.gamesPlayed = gamesPlayed;
  }
}
