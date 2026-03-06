package com.mipt.dbAPI.jpa.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "games_history")
public class GameHistoryEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Integer id;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "game_id", nullable = false)
  private GameEntity game;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "user_id", nullable = false)
  private UserEntity user;

  public Integer getId() {
    return id;
  }

  public GameEntity getGame() {
    return game;
  }

  public void setGame(GameEntity game) {
    this.game = game;
  }

  public UserEntity getUser() {
    return user;
  }

  public void setUser(UserEntity user) {
    this.user = user;
  }
}
