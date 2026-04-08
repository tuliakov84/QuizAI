package com.mipt.domainModel;

public class Topic {
  private Integer topicId;
  private String name;

  public Topic() {

  }

  public Topic(String name) {
    this.name = name;
  }

  public Integer getTopicId() {
    return topicId;
  }
  
  public void setTopicId(Integer topicId) {
    this.topicId = topicId;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }
}
