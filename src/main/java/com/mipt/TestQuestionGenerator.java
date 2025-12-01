package com.mipt;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class TestQuestionGenerator {
  public static void main(String[] args) {

    String testJson1 = "[{\"topic\": \"физика\", \"numberOfQuestions\": 2, \"difficult\": 2}]";
    runTest(testJson1);
  }

  private static void runTest(String jsonString) {
    try {
      CompletableFuture<String> future = QuestionGenerator.generate(jsonString);

      String result = future.get(1000, TimeUnit.SECONDS);

      try {
        System.out.println(result);

        // Сохраняем результат в файл
        saveToFile(result, "test_result_" + System.currentTimeMillis() + ".json");

      } catch (Exception e) {
        System.out.println("❌ Ошибка парсинга JSON: " + e.getMessage());
        System.out.println("Результат: " + result);
        e.printStackTrace();
      }

    } catch (TimeoutException e) {
      System.out.println("❌ Таймаут: генерация заняла слишком много времени");
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
    } catch (Exception e) {
      System.err.println("Ошибка при сохранении файла: " + e.getMessage());
    }
  }
}