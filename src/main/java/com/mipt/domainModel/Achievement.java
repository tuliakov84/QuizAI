package com.mipt.domainModel;

import com.mipt.domainModel.Interfaces.IAchievement;

public class Achievement implements IAchievement {
  private int achievementId;
  private String name;
  private boolean profilePicNeeded;
  private boolean descriptionNeeded;
  private int gamesNumberNeeded;
  private int globalPointsNeeded;
  private int globalRatingPlaceNeeded;
  private int currentGamePointsNeeded;
  private int currentGameRatingNeeded;
  private int currentGameDifficultyNeeded;

  public Achievement(int achievementId, String name) {
    this.achievementId = achievementId;
    this.name = name;
  }

  @Override
  public int getAchievementId() {
    return achievementId;
  }

  @Override
  public void setAchievementId(int achievementId) {
    this.achievementId = achievementId;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public void setName(String name) {
    this.name = name;
  }

  @Override
  public boolean isProfilePicNeeded() {
    return profilePicNeeded;
  }

  @Override
  public void setProfilePicNeeded(boolean profilePicNeeded) {
    this.profilePicNeeded = profilePicNeeded;
  }

  @Override
  public boolean isDescriptionNeeded() {
    return descriptionNeeded;
  }

  @Override
  public void setDescriptionNeeded(boolean descriptionNeeded) {
    this.descriptionNeeded = descriptionNeeded;
  }

  @Override
  public int getGamesNumberNeeded() {
    return gamesNumberNeeded;
  }

  @Override
  public void setGamesNumberNeeded(int gamesNumberNeeded) {
    this.gamesNumberNeeded = gamesNumberNeeded;
  }

  @Override
  public int getGlobalPointsNeeded() {
    return globalPointsNeeded;
  }

  @Override
  public void setGlobalPointsNeeded(int globalPointsNeeded) {
    this.globalPointsNeeded = globalPointsNeeded;
  }

  @Override
  public int getGlobalRatingPlaceNeeded() {
    return globalRatingPlaceNeeded;
  }

  @Override
  public void setGlobalRatingPlaceNeeded(int globalRatingPlaceNeeded) {
    this.globalRatingPlaceNeeded = globalRatingPlaceNeeded;
  }

  @Override
  public int getCurrentGamePointsNeeded() {
    return currentGamePointsNeeded;
  }

  @Override
  public void setCurrentGamePointsNeeded(int currentGamePointsNeeded) {
    this.currentGamePointsNeeded = currentGamePointsNeeded;
  }

  @Override
  public int getCurrentGameRatingNeeded() {
    return currentGameRatingNeeded;
  }

  @Override
  public void setCurrentGameRatingNeeded(int currentGameRatingNeeded) {
    this.currentGameRatingNeeded = currentGameRatingNeeded;
  }

  @Override
  public int getCurrentGameDifficultyNeeded() {
    return currentGameDifficultyNeeded;
  }

  @Override
  public void setCurrentGameDifficultyNeeded(int currentGameDifficultyNeeded) {
    this.currentGameDifficultyNeeded = currentGameDifficultyNeeded;
  }
}
