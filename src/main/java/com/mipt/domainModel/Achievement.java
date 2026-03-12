package com.mipt.domainModel;

public class Achievement {
  private Integer achievementId;

  private String name;
  private boolean profilePicNeeded;
  private boolean descriptionNeeded;
  private Integer gamesNumberNeeded;
  private Integer globalPointsNeeded;
  private Integer globalRatingPlaceNeeded;
  private Integer currentGamePointsNeeded;
  private Integer currentGameRatingNeeded;
  private Integer currentGameLevelDifficultyNeeded;

  public Achievement() {

  }

  public Achievement(String name) {
    this.name = name;
  }

  public Integer getAchievementId() {
    return achievementId;
  }
  
  public void setAchievementId(Integer achievementId) {
    this.achievementId = achievementId;
  }
  
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public boolean getProfilePicNeeded() {
    return profilePicNeeded;
  }
  
  public void setProfilePicNeeded(boolean profilePicNeeded) {
    this.profilePicNeeded = profilePicNeeded;
  }
  
  public boolean getDescriptionNeeded() {
    return descriptionNeeded;
  }
  
  public void setDescriptionNeeded(boolean descriptionNeeded) {
    this.descriptionNeeded = descriptionNeeded;
  }
  
  public Integer getGamesNumberNeeded() {
    return gamesNumberNeeded;
  }
  
  public void setGamesNumberNeeded(Integer gamesNumberNeeded) {
    this.gamesNumberNeeded = gamesNumberNeeded;
  }
  
  public Integer getGlobalPointsNeeded() {
    return globalPointsNeeded;
  }

  public void setGlobalPointsNeeded(Integer globalPointsNeeded) {
    this.globalPointsNeeded = globalPointsNeeded;
  }

  public Integer getGlobalRatingPlaceNeeded() {
    return globalRatingPlaceNeeded;
  }

  public void setGlobalRatingPlaceNeeded(Integer globalRatingPlaceNeeded) {
    this.globalRatingPlaceNeeded = globalRatingPlaceNeeded;
  }
  
  public Integer getCurrentGamePointsNeeded() {
    return currentGamePointsNeeded;
  }
  
  public void setCurrentGamePointsNeeded(Integer currentGamePointsNeeded) {
    this.currentGamePointsNeeded = currentGamePointsNeeded;
  }

  public Integer getCurrentGameRatingNeeded() {
    return currentGameRatingNeeded;
  }

  public void setCurrentGameRatingNeeded(Integer currentGameRatingNeeded) {
    this.currentGameRatingNeeded = currentGameRatingNeeded;
  }
  
  public Integer getCurrentGameLevelDifficultyNeeded() {
    return currentGameLevelDifficultyNeeded;
  }

  public void setCurrentGameLevelDifficultyNeeded(Integer currentGameLevelDifficultyNeeded) {
    this.currentGameLevelDifficultyNeeded = currentGameLevelDifficultyNeeded;
  }
}
