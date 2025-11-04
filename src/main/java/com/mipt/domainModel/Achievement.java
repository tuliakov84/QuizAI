package com.mipt.domainModel;

import com.mipt.domainModel.Interfaces.IAchievement;

public class Achievement implements IAchievement {
  public int achievementId;
  public String name;
  public boolean profilePicNeeded;
  public boolean descriptionNeeded;
  public int gamesNumberNeeded;
  public int globalPointsNeeded;
  public int globalRatingPlaceNeeded;
  public int currentGamePointsNeeded;
  public int currentGameRatingNeeded;
  public int currentGameLevelDifficultyNeeded;

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
  public int getCurrentGameLevelDifficultyNeeded() {
    return currentGameLevelDifficultyNeeded;
  }

  @Override
  public void setCurrentGameLevelDifficultyNeeded(int currentGameLevelDifficultyNeeded) {
    this.currentGameLevelDifficultyNeeded = currentGameLevelDifficultyNeeded;
  }
}
