package com.mipt.domainModel;

public class Achieved extends Achievement {
  private String session;
  private Integer achievementId;

  public String getSession() {
    return session;
  }
  public Integer getAchievementId() {
    return achievementId;
  }

  public void setSession(String session) {
    this.session = session;
  }

  public void setAchievementId(Integer achievementId) {
    this.achievementId = achievementId;
  }
}
