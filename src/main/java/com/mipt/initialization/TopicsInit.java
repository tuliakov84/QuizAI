package com.mipt.initialization;

import com.mipt.dbAPI.DbService;
import com.mipt.domainModel.Topic;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.FileReader;
import java.io.IOException;
import java.sql.SQLException;

public class TopicsInit {
  private final DbService dbService;

  public TopicsInit(DbService dbService) {
    this.dbService = dbService;
  }

  public void topicsInit() throws SQLException {
    Topic[] dbTopicList = dbService.getAllTopics();
    if (dbTopicList.length == 0) {
      JSONParser parser = new JSONParser();

      try (FileReader reader = new FileReader("topics.json")) {
        Object obj = parser.parse(reader);
        JSONObject jsonObject = (JSONObject) obj;
        
        String name = (String) jsonObject.get("name");
        long age = (long) jsonObject.get("age");

        System.out.println("Name: " + name);
        System.out.println("Age: " + age);

      } catch (IOException | ParseException e) {
        System.out.println(e.getMessage());
      }
    }
  }
}
