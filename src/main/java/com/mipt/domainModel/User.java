package com.mipt.domainModel;

import com.mipt.domainModel.Interfaces.IUser;

import java.time.Instant;

public class User implements IUser {
  public final int userId;
  public Game currentGame;
  public Achievement[] achievements;
  public Game[] gamesPlayed;

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

  @Override
  public int getUserId() {
    return 0;
  }

  @Override
  public String getUsername() {
    return username;
  }

  @Override
  public void setUsername(String newUsername) {
    this.username = newUsername;

  }

  @Override
  public String getPassword() {
    return password;
  }

  @Override
  public void setPassword(String password) {
    this.password = password;

  }

  @Override
  public int getPicId() {
    return picId;
  }

  @Override
  public void setPicId(int picId) {
    this.picId = picId;

  }

  @Override
  public String getDescription() {
    return description;
  }

  @Override
  public void setDescription(String description) {
    this.description = description;

  }

  @Override
  public Instant getLastActivity() {
    return lastActivity;
  }

  @Override
  public void setLastActivity(Instant lastActivity) {
    this.lastActivity = lastActivity;
  }

  @Override
  public int getGamesPlayedNumber() {
    return gamesPlayedNumber;
  }

  @Override
  public void setGamesPlayedNumber(int gamesPlayedNumber) {
    this.gamesPlayedNumber = gamesPlayedNumber;

  }

  @Override
  public int getGlobalPoints() {
    return globalPoints;
  }

  @Override
  public void setGlobalPoints(int globalPoints) {
    this.globalPoints = globalPoints;

  }

  @Override
  public int getGlobalPossiblePoints() {
    return globalPossiblePoints;
  }

  @Override
  public void setGlobalPossiblePoints(int globalPossiblePoints) {
    this.globalPossiblePoints = globalPossiblePoints;

  }

  @Override
  public int getCurrentGamePoints() {
    return currentGamePoints;
  }

  @Override
  public void setCurrentGamePoints(int currentGamePoints) {
    this.currentGamePoints = currentGamePoints;

  }

  @Override
  public Game getCurrentGame() {
    return currentGame;
  }

  @Override
  public void setCurrentGame(Game currentGame) {
    this.currentGame = currentGame;

  }

  @Override
  public Achievement[] getAchievements() {
    return achievements;
  }

  @Override
  public void setAchievements(Achievement[] achievements) {
    this.achievements = achievements;

  }

  @Override
  public Game[] getGamesPlayed() {
    return gamesPlayed;
  }

  @Override
  public void setGamesPlayed(Game[] gamesPlayed) {
    this.gamesPlayed = gamesPlayed;

  }
}
