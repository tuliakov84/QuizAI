package com.mipt.dbAPI.jpa.entity;

import com.mipt.bank.CoinTransactionType;
import jakarta.persistence.*;

import java.sql.Timestamp;

@Entity
@Table(name = "coin_transactions")
public class CoinTransactionEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Integer id;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "user_id", nullable = false)
  private UserEntity user;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "game_id")
  private GameEntity game;

  @Enumerated(EnumType.STRING)
  @Column(name = "transaction_type", nullable = false)
  private CoinTransactionType transactionType;

  @Column(name = "amount_delta", nullable = false)
  private Integer amountDelta;

  @Column(name = "balance_after", nullable = false)
  private Integer balanceAfter;

  @Column(name = "reason")
  private String reason;

  @Column(name = "created_at", nullable = false)
  private Timestamp createdAt;

  public Integer getId() {
    return id;
  }

  public UserEntity getUser() {
    return user;
  }

  public void setUser(UserEntity user) {
    this.user = user;
  }

  public GameEntity getGame() {
    return game;
  }

  public void setGame(GameEntity game) {
    this.game = game;
  }

  public CoinTransactionType getTransactionType() {
    return transactionType;
  }

  public void setTransactionType(CoinTransactionType transactionType) {
    this.transactionType = transactionType;
  }

  public Integer getAmountDelta() {
    return amountDelta;
  }

  public void setAmountDelta(Integer amountDelta) {
    this.amountDelta = amountDelta;
  }

  public Integer getBalanceAfter() {
    return balanceAfter;
  }

  public void setBalanceAfter(Integer balanceAfter) {
    this.balanceAfter = balanceAfter;
  }

  public String getReason() {
    return reason;
  }

  public void setReason(String reason) {
    this.reason = reason;
  }

  public Timestamp getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Timestamp createdAt) {
    this.createdAt = createdAt;
  }
}
