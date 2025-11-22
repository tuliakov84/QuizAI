package com.mipt.initialization;

import com.mipt.dbAPI.DbService;
import com.mipt.domainModel.Topic;

import java.sql.SQLException;

public class TopicsInit {
  private final DbService dbService;

  public TopicsInit(DbService dbService) {
    this.dbService = dbService;
  }

  public void topicsInit() {
    Topic topic = new Topic();
    topic.setName("My Topic");
    try {
      dbService.addTopic(topic);
    } catch (SQLException e) {
      return;
    }
  }
}
