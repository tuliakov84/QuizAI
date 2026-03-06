package com.mipt.dbAPI.jpa.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "achievements")
public class AchievementEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Integer id;

  @Column(name = "name")
  private String name;

  @Column(name = "profile_pic_needed")
  private Boolean profilePicNeeded;

  @Column(name = "description_needed")
  private Boolean descriptionNeeded;

  @Column(name = "games_number_needed")
  private Integer gamesNumberNeeded;

  @Column(name = "global_points_needed")
  private Integer globalPointsNeeded;

  @Column(name = "global_rating_place_needed")
  private Integer globalRatingPlaceNeeded;

  @Column(name = "current_game_points_needed")
  private Integer currentGamePointsNeeded;

  @Column(name = "current_game_rating_needed")
  private Integer currentGameRatingNeeded;

  @Column(name = "current_game_level_difficulty_needed")
  private Integer currentGameLevelDifficultyNeeded;

  public Integer getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Boolean getProfilePicNeeded() {
    return profilePicNeeded;
  }

  public void setProfilePicNeeded(Boolean profilePicNeeded) {
    this.profilePicNeeded = profilePicNeeded;
  }

  public Boolean getDescriptionNeeded() {
    return descriptionNeeded;
  }

  public void setDescriptionNeeded(Boolean descriptionNeeded) {
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
