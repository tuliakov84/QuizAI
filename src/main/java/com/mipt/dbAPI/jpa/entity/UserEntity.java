package com.mipt.dbAPI.jpa.entity;

import jakarta.persistence.*;

import java.sql.Timestamp;

@Entity
@Table(name = "users")
public class UserEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Integer id;

  @Column(name = "username")
  private String username;

  @Column(name = "password")
  private String password;

  @Column(name = "email")
  private String email;

  @Column(name = "session")
  private String session;

  @Column(name = "pic_id")
  private Integer picId;

  @Column(name = "custom_avatar_path")
  private String customAvatarPath;

  @Column(name = "description")
  private String description;

  @Column(name = "last_activity")
  private Timestamp lastActivity;

  @Column(name = "games_played_number")
  private Integer gamesPlayedNumber;

  @Column(name = "global_points")
  private Integer globalPoints;

  @Column(name = "global_possible_points")
  private Integer globalPossiblePoints;

  @Column(name = "current_game_points")
  private Integer currentGamePoints;

  @Column(name = "coin_balance")
  private Integer coinBalance;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "current_game_id")
  private GameEntity currentGame;

  public Integer getId() {
    return id;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
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

  public String getSession() {
    return session;
  }

  public void setSession(String session) {
    this.session = session;
  }

  public Integer getPicId() {
    return picId;
  }

  public void setPicId(Integer picId) {
    this.picId = picId;
  }

  public String getCustomAvatarPath() {
    return customAvatarPath;
  }

  public void setCustomAvatarPath(String customAvatarPath) {
    this.customAvatarPath = customAvatarPath;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Timestamp getLastActivity() {
    return lastActivity;
  }

  public void setLastActivity(Timestamp lastActivity) {
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

  public GameEntity getCurrentGame() {
    return currentGame;
  }

  public void setCurrentGame(GameEntity currentGame) {
    this.currentGame = currentGame;
  }
}
