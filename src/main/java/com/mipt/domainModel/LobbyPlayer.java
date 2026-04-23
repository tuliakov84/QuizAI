package com.mipt.domainModel;

public class LobbyPlayer {
  private String username;
  private Integer picId;
  private String avatarUrl;
  private Boolean premiumActive;

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public Integer getPicId() {
    return picId;
  }

  public void setPicId(Integer picId) {
    this.picId = picId;
  }

  public String getAvatarUrl() {
    return avatarUrl;
  }

  public void setAvatarUrl(String avatarUrl) {
    this.avatarUrl = avatarUrl;
  }

  public Boolean getPremiumActive() {
    return premiumActive;
  }

  public void setPremiumActive(Boolean premiumActive) {
    this.premiumActive = premiumActive;
  }
}
