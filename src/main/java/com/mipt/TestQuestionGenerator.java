package com.mipt;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.concurrent.CompletableFuture;

public class TestQuestionGenerator {
  public static void main(String[] args) {
    System.out.println("Тестирование QuestionGenerator...\n");

    // Тест 1: Простой запрос
    System.out.println("Тест 1: Генерация 2 вопросов по физике средней сложности");
    String testJson1 = "[{\"topic\": \"космос\", \"n\": 10, \"difficult\": 3}]";
    runTest(testJson1);

    // Тест 2: Другой запрос
    System.out.println("\n\nТест 2: Генерация 3 вопросов по биологии легкой сложности");
    String testJson2 = "[{\"topic\": \"биология\", \"n\": 3, \"difficult\": 1}]";
    runTest(testJson2);
  }

  private static void runTest(String jsonString) {
    System.out.println("Запрос: " + jsonString);

    try {
      CompletableFuture<String> future = QuestionGenerator.generate(jsonString);
      String result = future.get(); // Ждем результат

      System.out.println("\nРезультат:");
      System.out.println("-".repeat(50));

      if (result.equals("{\"questions\": []}")) {
        System.out.println("❌ Получен пустой результат");
        return;
      }

      try {
        JSONObject json = new JSONObject(result);
        JSONArray questions = json.getJSONArray("questions");

        System.out.println("✅ Успешно! Сгенерировано вопросов: " + questions.length());
        System.out.println("\nПример первого вопроса:");

        if (questions.length() > 0) {
          JSONObject firstQuestion = questions.getJSONObject(0);
          System.out.println("  Номер: " + firstQuestion.getInt("number"));
          System.out.println("  Вопрос: " + firstQuestion.getString("question"));
          System.out.println("  Правильный ответ: " + firstQuestion.getInt("right_ans_index"));

          JSONArray answers = firstQuestion.getJSONArray("available_answers");
          System.out.println("  Варианты ответов:");
          for (int i = 0; i < answers.length(); i++) {
            JSONObject answer = answers.getJSONObject(i);
            System.out.println("    " + answer.getInt("index") + ": " +
                answer.getString("answer").substring(0,
                    Math.min(40, answer.getString("answer").length())) +
                (answer.getString("answer").length() > 40 ? "..." : ""));
          }
        }

        // Сохраняем результат в файл
        saveToFile(result, "test_result_" + System.currentTimeMillis() + ".json");

      } catch (Exception e) {
        System.out.println("❌ Ошибка парсинга JSON: " + e.getMessage());
        System.out.println("Сырой результат (первые 300 символов):");
        System.out.println(result.substring(0, Math.min(300, result.length())));
      }

    } catch (Exception e) {
      System.out.println("❌ Ошибка выполнения: " + e.getMessage());
      e.printStackTrace();
    }
  }

  private static void saveToFile(String jsonContent, String filename) {
    try {
      java.nio.file.Files.write(
          java.nio.file.Paths.get(filename),
          jsonContent.getBytes(java.nio.charset.StandardCharsets.UTF_8)
      );
      System.out.println("\n💾 Результат сохранен в файл: " + filename);
    } catch (Exception e) {
      System.err.println("Ошибка при сохранении файла: " + e.getMessage());
    }
  }
}