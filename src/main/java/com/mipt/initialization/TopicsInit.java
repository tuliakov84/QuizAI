package com.mipt.initialization;

import com.mipt.dbAPI.DbService;
import com.mipt.domainModel.Topic;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;

public class TopicsInit {
  private DbService dbService;

  public TopicsInit(DbService dbService) {
    this.dbService = dbService;
  }

  public void topicsInit() {
    try {
      Topic[] dbTopicList = dbService.getAllTopics();

      if (dbTopicList.length == 0) {
        JSONParser parser = new JSONParser();

        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("topics.json")) {
          if (inputStream == null) {
            throw new FileNotFoundException("File topics.json not found in classpath");
          }

          try (InputStreamReader reader = new InputStreamReader(inputStream)) {
            Object globalObj = parser.parse(reader);
            JSONArray jsonArray = (JSONArray) globalObj;
            for (Object element : jsonArray) {
              JSONObject obj = (JSONObject) element;
              Topic topic = new Topic((String) obj.get("name"));
              dbService.addTopic(topic);
            }
          }
        } catch (IOException | ParseException e) {
          System.out.println("Error reading topics.json: " + e.getMessage());
        }
      }
    } catch (Exception e) {
      System.out.println("Error during topics initialization: " + e.getMessage());
    }
  }
}
