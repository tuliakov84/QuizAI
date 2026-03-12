package com.mipt.domainModel;

public class Achieved extends Achievement {
  private String session;
  private int achievementId;

  public String getSession() {
    return session;
  }
  public int getAchievementId() {
    return achievementId;
  }

  public void setSession(String session) {
    this.session = session;
  }

  public void setAchievementId(int achievementId) {
    this.achievementId = achievementId;
  }
}
