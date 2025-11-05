package com.mipt.domainModel;

public class Achievement {
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

  public int getAchievementId() {
    return achievementId;
  }
  
  public void setAchievementId(int achievementId) {
    this.achievementId = achievementId;
  }
  
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public boolean isProfilePicNeeded() {
    return profilePicNeeded;
  }
  
  public void setProfilePicNeeded(boolean profilePicNeeded) {
    this.profilePicNeeded = profilePicNeeded;
  }
  
  public boolean isDescriptionNeeded() {
    return descriptionNeeded;
  }
  
  public void setDescriptionNeeded(boolean descriptionNeeded) {
    this.descriptionNeeded = descriptionNeeded;
  }
  
  public int getGamesNumberNeeded() {
    return gamesNumberNeeded;
  }
  
  public void setGamesNumberNeeded(int gamesNumberNeeded) {
    this.gamesNumberNeeded = gamesNumberNeeded;
  }
  
  public int getGlobalPointsNeeded() {
    return globalPointsNeeded;
  }

  public void setGlobalPointsNeeded(int globalPointsNeeded) {
    this.globalPointsNeeded = globalPointsNeeded;
  }

  public int getGlobalRatingPlaceNeeded() {
    return globalRatingPlaceNeeded;
  }

  public void setGlobalRatingPlaceNeeded(int globalRatingPlaceNeeded) {
    this.globalRatingPlaceNeeded = globalRatingPlaceNeeded;
  }
  
  public int getCurrentGamePointsNeeded() {
    return currentGamePointsNeeded;
  }
  
  public void setCurrentGamePointsNeeded(int currentGamePointsNeeded) {
    this.currentGamePointsNeeded = currentGamePointsNeeded;
  }

  public int getCurrentGameRatingNeeded() {
    return currentGameRatingNeeded;
  }

  public void setCurrentGameRatingNeeded(int currentGameRatingNeeded) {
    this.currentGameRatingNeeded = currentGameRatingNeeded;
  }
  
  public int getCurrentGameLevelDifficultyNeeded() {
    return currentGameLevelDifficultyNeeded;
  }

  public void setCurrentGameLevelDifficultyNeeded(int currentGameLevelDifficultyNeeded) {
    this.currentGameLevelDifficultyNeeded = currentGameLevelDifficultyNeeded;
  }
}
