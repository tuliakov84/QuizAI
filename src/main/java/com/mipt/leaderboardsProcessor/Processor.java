package com.mipt.leaderboardsProcessor;


import org.json.JSONArray;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.sql.SQLException;

public class Processor {
  public static void process() throws IOException, SQLException {
    LeaderboardsLoader loader = new LeaderboardsLoader(
      "jdbc:postgresql://localhost:5432/quizai",
      "postgres",
      "postgres"
    );

    JSONArray data = loader.getGlobalLeaderboards();

    // Определяем путь к файлу в resources
    Path outputPath = Paths.get("src/main/resources/global-leaderboards.json");

    // Создаем директории, если они не существуют
    Files.createDirectories(outputPath.getParent());

    // Сохраняем данные
    String jsonString = data.toString(2);
    Files.writeString(outputPath, jsonString, StandardCharsets.UTF_8);

    System.out.println("Saved leaderboards to: " + outputPath.toAbsolutePath());
  }
}