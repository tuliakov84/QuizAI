/**
 * ACHIEVEMENTS INITIALIZER
 * Инициализация достижений при первом запуске приложения/отсутствии данных
 * Данные конфигурации берутся из файла resources/achievements.json
 */

package com.mipt.initialization;

import com.mipt.dbAPI.DbService;
import com.mipt.domainModel.Achievement;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.util.HashSet;
import java.util.Set;

public class AchievementsInit {
  private DbService dbService;

  public AchievementsInit(DbService dbService) {
    this.dbService = dbService;
  }

  public void achievementsInit() {
    try {
      JSONParser parser = new JSONParser();
      // Получаем все существующие достижения из БД
      Achievement[] dbAchievements = dbService.getAllAchievements();
      Set<String> existingNames = new HashSet<>();
      for (Achievement a : dbAchievements) {
        existingNames.add(a.getName());
      }

      // Загружаем достижения из файла
      try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("achievements.json")) {
        if (inputStream == null) {
          System.err.println("Файл achievements.json не найден в classpath");
          return;
        }
        try (InputStreamReader reader = new InputStreamReader(inputStream)) {
          Object globalObj = parser.parse(reader);
          JSONArray jsonArray = (JSONArray) globalObj;
          for (Object element : jsonArray) {
            JSONObject obj = (JSONObject) element;
            String name = (String) obj.get("name");
            // Добавляем только если такого имени ещё нет
            if (!existingNames.contains(name)) {
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
              System.out.println("Добавлено достижение: " + name);
            }
          }
        } catch (IOException | ParseException e) {
          System.err.println("Ошибка чтения achievements.json: " + e.getMessage());
        }
      }
    } catch (Exception e) {
      System.err.println("Ошибка инициализации достижений: " + e.getMessage());
    }
  }
}