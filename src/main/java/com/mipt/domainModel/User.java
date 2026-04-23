package com.mipt.domainModel;

import java.time.Instant;
import java.util.List;

public class User {
  private Integer userId;
  private Game currentGame;
  private Integer[] achievements;
  private Integer[] gamesPlayed;

  private String session;
  private String username;
  private String password;
  private String email;
  private Integer picId;
  private String customAvatarPath;
  private String avatarUrl;
  private String description;
  private Instant lastActivity;
  private Integer gamesPlayedNumber;
  private Integer globalPoints;
  private Integer globalPossiblePoints;
  private Integer currentGamePoints;
  private Integer coinBalance;
  private Instant premiumUntil;
  private Boolean premiumActive;

  public User() {

  }

  public User(Integer userId, String password, String username) {
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

  public void setUserId(Integer userId) {
    this.userId = userId;
  }
  
  public Integer getUserId() {
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

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public Integer getPicId() {
    return picId;
  }

  public void setPicId(Integer picId) {
    this.picId = picId;

  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;

  }

  public String getCustomAvatarPath() {
    return customAvatarPath;
  }

  public void setCustomAvatarPath(String customAvatarPath) {
    this.customAvatarPath = customAvatarPath;
  }

  public String getAvatarUrl() {
    return avatarUrl;
  }

  public void setAvatarUrl(String avatarUrl) {
    this.avatarUrl = avatarUrl;
  }

  public Instant getLastActivity() {
    return lastActivity;
  }

  public void setLastActivity(Instant lastActivity) {
    this.lastActivity = lastActivity;
  }

  public Integer getGamesPlayedNumber() {
    return gamesPlayedNumber;
  }

  public void setGamesPlayedNumber(Integer gamesPlayedNumber) {
    this.gamesPlayedNumber = gamesPlayedNumber;

  }

  public Integer getGlobalPoints() {
    return globalPoints;
  }

  public void setGlobalPoints(Integer globalPoints) {
    this.globalPoints = globalPoints;

  }

  public Integer getGlobalPossiblePoints() {
    return globalPossiblePoints;
  }

  public void setGlobalPossiblePoints(Integer globalPossiblePoints) {
    this.globalPossiblePoints = globalPossiblePoints;

  }

  public Integer getCurrentGamePoints() {
    return currentGamePoints;
  }

  public void setCurrentGamePoints(Integer currentGamePoints) {
    this.currentGamePoints = currentGamePoints;

  }

  public Integer getCoinBalance() {
    return coinBalance;
  }

  public void setCoinBalance(Integer coinBalance) {
    this.coinBalance = coinBalance;
  }

  public Instant getPremiumUntil() {
    return premiumUntil;
  }

  public void setPremiumUntil(Instant premiumUntil) {
    this.premiumUntil = premiumUntil;
  }

  public Boolean getPremiumActive() {
    return premiumActive;
  }

  public void setPremiumActive(Boolean premiumActive) {
    this.premiumActive = premiumActive;
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
