package com.mipt.domainModel;

public class Topic {
  public int topicId;
  public String name;

  public Topic(int topicId, String name) {
    this.topicId = topicId;
    this.name = name;
  }

  public int getTopicId() {
    return topicId;
  }
  
  public void setTopicId(int topicId) {
    this.topicId = topicId;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }
}
