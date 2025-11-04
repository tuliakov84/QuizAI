package com.mipt.domainModel.Interfaces;

public interface IAchievement {
  int getAchievementId();
  void setAchievementId(int achievementId);

  String getName();
  void setName(String name);

  boolean isProfilePicNeeded();
  void setProfilePicNeeded(boolean profilePicNeeded);

  boolean isDescriptionNeeded();
  void setDescriptionNeeded(boolean descriptionNeeded);

  int getGamesNumberNeeded();
  void setGamesNumberNeeded(int gamesNumberNeeded);

  int getGlobalPointsNeeded();
  void setGlobalPointsNeeded(int globalPointsNeeded);

  int getGlobalRatingPlaceNeeded();
  void setGlobalRatingPlaceNeeded(int globalRatingPlaceNeeded);

  int getCurrentGamePointsNeeded();
  void setCurrentGamePointsNeeded(int currentGamePointsNeeded);

  int getCurrentGameRatingNeeded();
  void setCurrentGameRatingNeeded(int currentGameRatingNeeded);

  int getCurrentGameLevelDifficultyNeeded();
  void setCurrentGameLevelDifficultyNeeded(int currentGameLevelDifficultyNeeded);
}
