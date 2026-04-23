package com.mipt.domainModel;

public class PremiumPurchaseRequest {
  private String session;
  private Integer days;

  public String getSession() {
    return session;
  }

  public void setSession(String session) {
    this.session = session;
  }

  public Integer getDays() {
    return days;
  }

  public void setDays(Integer days) {
    this.days = days;
  }
}
