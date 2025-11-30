package com.mipt.initialization;

import com.mipt.dbAPI.DbService;
import com.mipt.domainModel.Achievement;
import com.mipt.domainModel.Topic;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class AchievementsInit {
  private DbService dbService;

  public AchievementsInit(DbService dbService) {
    this.dbService = dbService;
  }

  public void achievementsInit() {
    try {
      JSONParser parser = new JSONParser();
      Achievement[] dbAchievementsList = dbService.getAllAchievements();

      try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("achievements.json")) {
        if (inputStream == null) {
          throw new FileNotFoundException("File achievements.json not found in classpath");
        }

        try (InputStreamReader reader = new InputStreamReader(inputStream)) {
          Object globalObj = parser.parse(reader);
          JSONArray jsonArray = (JSONArray) globalObj;
          if (jsonArray.size() > dbAchievementsList.length) {
            for (Object element : jsonArray) {
              JSONObject obj = (JSONObject) element;
              String name = (String) obj.get("name");
              Achievement achievement = new Achievement(name);
              achievement.setProfilePicNeeded((Boolean) obj.get("profilePicNeeded"));
              achievement.setDescriptionNeeded((Boolean) obj.get("descriptionNeeded"));
              achievement.setGamesNumberNeeded(((Number) obj.get("gamesNumberNeeded")).intValue());
              achievement.setGlobalPointsNeeded(((Number) obj.get("globalPointsNeeded")).intValue());
              achievement.setGlobalRatingPlaceNeeded(((Number) obj.get("globalRatingPlaceNeeded")).intValue());
              achievement.setCurrentGamePointsNeeded(((Number) obj.get("currentGamePointsNeeded")).intValue());
              achievement.setCurrentGameRatingNeeded(((Number) obj.get("currentGameRatingNeeded")).intValue());
              achievement.setCurrentGameLevelDifficultyNeeded(((Number) obj.get("currentGameLevelDifficultyNeeded")).intValue());
              dbService.addAchievement(achievement);
            }
          }
        } catch (IOException | ParseException e) {
          System.out.println("Error reading achievements.json: " + e.getMessage());
        }
      }
    } catch (Exception e) {
      System.out.println("Error during achievements initialization: " + e.getMessage());
    }
  }
}
