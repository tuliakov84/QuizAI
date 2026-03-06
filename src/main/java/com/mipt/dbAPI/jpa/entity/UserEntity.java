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

  @Column(name = "session")
  private String session;

  @Column(name = "pic_id")
  private int picId;

  @Column(name = "description")
  private String description;

  @Column(name = "last_activity")
  private Timestamp lastActivity;

  @Column(name = "games_played_number")
  private int gamesPlayedNumber;

  @Column(name = "global_points")
  private int globalPoints;

  @Column(name = "global_possible_points")
  private int globalPossiblePoints;

  @Column(name = "current_game_points")
  private int currentGamePoints;

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

  public String getSession() {
    return session;
  }

  public void setSession(String session) {
    this.session = session;
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

  public Timestamp getLastActivity() {
    return lastActivity;
  }

  public void setLastActivity(Timestamp lastActivity) {
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

  public GameEntity getCurrentGame() {
    return currentGame;
  }

  public void setCurrentGame(GameEntity currentGame) {
    this.currentGame = currentGame;
  }
}
