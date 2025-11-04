package com.mipt.domainModel;

import com.mipt.domainModel.Interfaces.ITopic;

public class Topic implements ITopic {
  public int topicId;
  public String name;

  public Topic(int topicId, String name) {
    this.topicId = topicId;
    this.name = name;
  }

  @Override
  public int getTopicId() {
    return topicId;
  }

  @Override
  public void setTopicId(int topicId) {
    this.topicId = topicId;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public void setName(String name) {
    this.name = name;
  }
}
