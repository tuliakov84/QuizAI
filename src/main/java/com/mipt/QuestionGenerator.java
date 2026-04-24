package com.mipt;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public class QuestionGenerator {
  private static final String GAME_MODE_CASUAL = "CASUAL";
  private static final String GAME_MODE_TRUE_FALSE = "TRUE_FALSE";

  private static final String DEFAULT_LLM_URL = "http://localhost:11434/api/chat";
  private static final String DEFAULT_LLM_MODEL = "qwen2.5";
  private static final int DEFAULT_CONNECT_TIMEOUT_SECONDS = 30;
  private static final int DEFAULT_REQUEST_TIMEOUT_SECONDS = 120;
  private static final int DEFAULT_MAX_RETRIES_PER_QUESTION = 3;
  private static final double DEFAULT_TEMPERATURE = 0.5;
  private static final int DEFAULT_NUM_PREDICT = 1024;

  private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
      .connectTimeout(Duration.ofSeconds(readIntConfig("app.llm.connect-timeout-seconds", "APP_LLM_CONNECT_TIMEOUT_SECONDS", DEFAULT_CONNECT_TIMEOUT_SECONDS)))
      .build();

  private record GenerationInput(
      String topic,
      int numberOfQuestions,
      int difficult,
      String gameMode,
      List<Integer> targetQuestionNumbers,
      List<String> existingQuestionTexts
  ) {
  }

  private record GenerationContext(
      String topic,
      int difficult,
      String gameMode,
      List<Integer> targetQuestionNumbers,
      int maxRetriesPerQuestion,
      Set<String> usedQuestionKeys,
      StringBuilder previousQuestions
  ) {
  }

  public static CompletableFuture<String> generate(String jsonString) {
    GenerationInput input = parseInput(jsonString);
    Set<String> usedQuestionKeys = new HashSet<>();

    for (String existingText : input.existingQuestionTexts) {
      String normalized = normalizeQuestionText(existingText);
      if (!normalized.isEmpty()) {
        usedQuestionKeys.add(normalized);
      }
    }

    StringBuilder previousQuestions = new StringBuilder();
    for (String existing : input.existingQuestionTexts) {
      if (existing != null && !existing.isBlank()) {
        previousQuestions.append("- ").append(existing.trim()).append("\n");
      }
    }

    GenerationContext context = new GenerationContext(
        input.topic,
        input.difficult,
        input.gameMode,
        input.targetQuestionNumbers,
        readIntConfig("app.llm.max-retries-per-question", "APP_LLM_MAX_RETRIES_PER_QUESTION", DEFAULT_MAX_RETRIES_PER_QUESTION),
        usedQuestionKeys,
        previousQuestions
    );

    return generateRecursively(context, 0, input.numberOfQuestions, 0, new JSONArray())
        .thenApply(JSONArray::toString);
  }

  private static CompletableFuture<JSONArray> generateRecursively(
      GenerationContext context,
      int currentIndex,
      int totalQuestions,
      int retryCount,
      JSONArray allQuestions
  ) {
    if (currentIndex >= totalQuestions) {
      return CompletableFuture.completedFuture(allQuestions);
    }

    int targetNumber = context.targetQuestionNumbers.isEmpty()
        ? currentIndex + 1
        : context.targetQuestionNumbers.get(currentIndex);

    if (retryCount > context.maxRetriesPerQuestion) {
      return CompletableFuture.failedFuture(new IllegalStateException(
          "Failed to generate a valid unique question after "
              + context.maxRetriesPerQuestion + " retries. questionNumber=" + targetNumber
      ));
    }

    return generateSingleQuestion(context, targetNumber)
        .thenCompose(generatedQuestion -> {
          if (generatedQuestion == null) {
            return generateRecursively(context, currentIndex, totalQuestions, retryCount + 1, allQuestions);
          }

          registerQuestionAndContext(generatedQuestion, context.previousQuestions, context.usedQuestionKeys);
          allQuestions.put(generatedQuestion);
          return generateRecursively(context, currentIndex + 1, totalQuestions, 0, allQuestions);
        });
  }

  private static CompletableFuture<JSONObject> generateSingleQuestion(GenerationContext context, int questionNumber) {
    String prompt = buildPrompt(context, questionNumber);

    JSONObject payload = new JSONObject();
    payload.put("model", readStringConfig("app.llm.model", "APP_LLM_MODEL", DEFAULT_LLM_MODEL));
    payload.put("format", "json");
    payload.put("stream", false);

    JSONObject options = new JSONObject();
    options.put("temperature", readDoubleConfig("app.llm.temperature", "APP_LLM_TEMPERATURE", DEFAULT_TEMPERATURE));
    options.put("num_predict", readIntConfig("app.llm.num-predict", "APP_LLM_NUM_PREDICT", DEFAULT_NUM_PREDICT));
    payload.put("options", options);

    JSONArray messages = new JSONArray();
    JSONObject m = new JSONObject();
    m.put("role", "user");
    m.put("content", prompt);
    messages.put(m);

    payload.put("messages", messages);

    HttpRequest req = HttpRequest.newBuilder()
        .uri(URI.create(readStringConfig("app.llm.url", "APP_LLM_URL", DEFAULT_LLM_URL)))
        .timeout(Duration.ofSeconds(readIntConfig("app.llm.request-timeout-seconds", "APP_LLM_REQUEST_TIMEOUT_SECONDS", DEFAULT_REQUEST_TIMEOUT_SECONDS)))
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(payload.toString(), StandardCharsets.UTF_8))
        .build();

    return HTTP_CLIENT.sendAsync(req, HttpResponse.BodyHandlers.ofString())
        .thenApply(response -> {
          if (response.statusCode() != 200) {
            System.err.println("LLM API returned non-200 status: " + response.statusCode());
            return null;
          }

          try {
            JSONObject responseObj = new JSONObject(response.body());
            JSONObject messageObj = responseObj.getJSONObject("message");
            String content = messageObj.getString("content").trim();

            String jsonContent = extractValidJson(content);
            if (jsonContent == null) {
              System.err.println("Не удалось извлечь JSON для вопроса " + questionNumber);
              return null;
            }

            JSONObject questionObj = new JSONObject(jsonContent);
            JSONObject fixedQuestion = fixQuestionStructure(questionObj, questionNumber, context.gameMode);

            String questionText = fixedQuestion.optString("question_text", "");
            String normalized = normalizeQuestionText(questionText);
            if (normalized.isEmpty() || context.usedQuestionKeys.contains(normalized)) {
              return null;
            }
            return fixedQuestion;

          } catch (Exception e) {
            System.err.println("Failed to parse LLM question " + questionNumber + ": " + e.getMessage());
            return null;
          }
        })
        .exceptionally(ex -> {
          System.err.println("LLM request failed for question " + questionNumber + ": " + ex.getMessage());
          return null;
        });
  }

  private static GenerationInput parseInput(String jsonString) {
    try {
      JSONArray inputArray = new JSONArray(jsonString);
      if (inputArray.isEmpty()) {
        throw new IllegalArgumentException("Generator payload array is empty");
      }
      JSONObject obj = inputArray.getJSONObject(0);

      String topic = obj.getString("topic").trim();
      int numberOfQuestions = obj.getInt("numberOfQuestions");
      int difficult = obj.getInt("difficult");
      String gameMode = normalizeGameMode(obj.optString("gameMode", GAME_MODE_CASUAL));

      if (topic.isEmpty()) {
        throw new IllegalArgumentException("Topic must not be empty");
      }
      if (numberOfQuestions <= 0) {
        throw new IllegalArgumentException("numberOfQuestions must be > 0");
      }

      JSONArray questionNumbersToRegenerate = obj.optJSONArray("questionNumbersToRegenerate");
      List<Integer> targetNumbers = new ArrayList<>();
      if (questionNumbersToRegenerate != null && !questionNumbersToRegenerate.isEmpty()) {
        for (int i = 0; i < questionNumbersToRegenerate.length(); i++) {
          targetNumbers.add(questionNumbersToRegenerate.getInt(i));
        }
      }
      if (!targetNumbers.isEmpty()) {
        numberOfQuestions = targetNumbers.size();
      }

      JSONArray existingQuestionsArray = obj.optJSONArray("existingQuestions");
      List<String> existingQuestionTexts = new ArrayList<>();
      if (existingQuestionsArray != null && !existingQuestionsArray.isEmpty()) {
        for (int i = 0; i < existingQuestionsArray.length(); i++) {
          String questionText = existingQuestionsArray.optString(i, "").trim();
          if (!questionText.isEmpty()) {
            existingQuestionTexts.add(questionText);
          }
        }
      }

      return new GenerationInput(topic, numberOfQuestions, difficult, gameMode, targetNumbers, existingQuestionTexts);
    } catch (Exception e) {
      throw new CompletionException("Invalid generator payload: " + e.getMessage(), e);
    }
  }

  private static String buildPrompt(GenerationContext context, int questionNumber) {
    String uniquenessContext = context.previousQuestions.isEmpty()
        ? "Нет ранее сгенерированных вопросов."
        : "Запрещено повторять или перефразировать следующие вопросы:\n" + context.previousQuestions;
    String modeHint = context.targetQuestionNumbers.isEmpty()
        ? "Режим: первичная генерация"
        : "Режим: перегенерация отдельных вопросов";
    boolean trueFalseMode = isTrueFalseMode(context.gameMode);

    String formatExample = trueFalseMode
        ? "{\n" +
            "  \"question_number\": " + questionNumber + ",\n" +
            "  \"question_text\": \"...\",\n" +
            "  \"available_answers\": [\n" +
            "    {\"index\": 1, \"answer\": \"Правда\"},\n" +
            "    {\"index\": 2, \"answer\": \"Ложь\"},\n" +
            "    {\"index\": 3, \"answer\": \"\"},\n" +
            "    {\"index\": 4, \"answer\": \"\"}\n" +
            "  ],\n" +
            "  \"right_answer_number\": 1..2\n" +
            "}"
        : "{\n" +
            "  \"question_number\": " + questionNumber + ",\n" +
            "  \"question_text\": \"...\",\n" +
            "  \"available_answers\": [\n" +
            "    {\"index\": 1, \"answer\": \"...\"},\n" +
            "    {\"index\": 2, \"answer\": \"...\"},\n" +
            "    {\"index\": 3, \"answer\": \"...\"},\n" +
            "    {\"index\": 4, \"answer\": \"...\"}\n" +
            "  ],\n" +
            "  \"right_answer_number\": 1..4\n" +
            "}";
    String answerConstraints = trueFalseMode
        ? "3) Это режим Правда/Ложь: варианты ответов должны быть только «Правда» и «Ложь» в таком порядке.\n" +
            "4) Для index 3 и 4 верни пустые строки.\n" +
            "5) Правильный ответ может быть только 1 или 2.\n" +
            "6) Не используй вопросы из списка запрета.\n" +
            "7) Не добавляй текст вне JSON."
        : "3) Варианты ответов правдоподобные и различающиеся по смыслу.\n" +
            "4) Не используй вопросы из списка запрета.\n" +
            "5) Не добавляй текст вне JSON.";
    String gameModeHint = trueFalseMode
        ? "Игровой режим: Правда/Ложь."
        : "Игровой режим: стандартный тест с 4 вариантами ответа.";

    return String.format(
        "Ты генерируешь вопрос для онлайн-квиза.\n" +
            "%s\n" +
            "%s\n" +
            "%s\n" +
            "Тема: %s\n" +
            "Сложность: %d (1 = легко, 2 = средне, 3 = сложно).\n" +
            "Номер вопроса: %d\n\n" +
            "Верни строго один JSON-объект БЕЗ markdown и без дополнительных пояснений.\n" +
            "Формат строго такой:\n" +
            "%s\n\n" +
            "Ограничения:\n" +
            "1) Только русский язык.\n" +
            "2) Только один правильный ответ.\n" +
            "%s",
        modeHint,
        uniquenessContext,
        gameModeHint,
        context.topic,
        context.difficult,
        questionNumber,
        formatExample,
        answerConstraints
    );
  }

  private static void registerQuestionAndContext(
      JSONObject question,
      StringBuilder previousQuestions,
      Set<String> usedQuestionKeys
  ) {
    String questionText = question.optString("question_text", "").trim();
    if (!questionText.isEmpty()) {
      String normalized = normalizeQuestionText(questionText);
      if (!normalized.isEmpty()) {
        usedQuestionKeys.add(normalized);
      }
      previousQuestions.append("- ").append(questionText).append("\n");
    }
  }

  private static String extractValidJson(String content) {
    if (content == null || content.isEmpty()) {
      return null;
    }

    content = content.trim();

    int start = content.indexOf('{');
    if (start == -1) {
      return null;
    }

    int braceBalance = 0;
    boolean inQuotes = false;
    char prevChar = 0;

    for (int i = start; i < content.length(); i++) {
      char c = content.charAt(i);

      if (c == '"' && prevChar != '\\') {
        inQuotes = !inQuotes;
      } else if (!inQuotes) {
        if (c == '{') braceBalance++;
        else if (c == '}') braceBalance--;
      }

      prevChar = c;

      if (braceBalance == 0 && i > start) {
        return content.substring(start, i + 1);
      }
    }

    return null;
  }

  private static String normalizeQuestionText(String text) {
    if (text == null) {
      return "";
    }
    return text
        .toLowerCase()
        .replaceAll("[\\p{Punct}]", " ")
        .replaceAll("\\s+", " ")
        .trim();
  }

  private static String readStringConfig(String sysProperty, String envName, String defaultValue) {
    String fromSystem = System.getProperty(sysProperty);
    if (fromSystem != null && !fromSystem.isBlank()) {
      return fromSystem.trim();
    }
    String fromEnv = System.getenv(envName);
    if (fromEnv != null && !fromEnv.isBlank()) {
      return fromEnv.trim();
    }
    return defaultValue;
  }

  private static int readIntConfig(String sysProperty, String envName, int defaultValue) {
    String raw = readStringConfig(sysProperty, envName, String.valueOf(defaultValue));
    try {
      return Integer.parseInt(raw);
    } catch (NumberFormatException e) {
      return defaultValue;
    }
  }

  private static double readDoubleConfig(String sysProperty, String envName, double defaultValue) {
    String raw = readStringConfig(sysProperty, envName, String.valueOf(defaultValue));
    try {
      return Double.parseDouble(raw);
    } catch (NumberFormatException e) {
      return defaultValue;
    }
  }

  private static JSONObject fixQuestionStructure(JSONObject question, int expectedNumber, String gameMode) {
    try {
      JSONObject fixed = new JSONObject();
      fixed.put("question_number", expectedNumber);
      boolean trueFalseMode = isTrueFalseMode(gameMode);

      if (question.has("question_text")) {
        fixed.put("question_text", question.getString("question_text"));
      } else if (question.has("question")) {
        fixed.put("question_text", question.getString("question"));
      } else {
        fixed.put("question_text", "Вопрос на тему " + expectedNumber);
      }

      JSONArray answers = new JSONArray();
      if (trueFalseMode) {
        answers.put(new JSONObject().put("index", 1).put("answer", "Правда"));
        answers.put(new JSONObject().put("index", 2).put("answer", "Ложь"));
        answers.put(new JSONObject().put("index", 3).put("answer", ""));
        answers.put(new JSONObject().put("index", 4).put("answer", ""));
      } else if (question.has("available_answers")) {
        JSONArray originalAnswers = question.getJSONArray("available_answers");
        for (int i = 0; i < Math.min(4, originalAnswers.length()); i++) {
          JSONObject answerObj = originalAnswers.getJSONObject(i);
          JSONObject fixedAnswer = new JSONObject();
          fixedAnswer.put("index", i + 1);

          if (answerObj.has("answer")) {
            String answerText = answerObj.getString("answer");
            answerText = answerText.replaceAll("\"Впитер\"", "Юпитер")
                .replaceAll("\"Ипитер\"", "Юпитер")
                .replaceAll("\"\\\\\"", "\"") // Убираем экранированные кавычки
                .replaceAll("^\"|\"$", "")
                .trim();
            fixedAnswer.put("answer", answerText);
          } else {
            fixedAnswer.put("answer", "Вариант " + (i + 1));
          }

          answers.put(fixedAnswer);
        }
      }

      while (answers.length() < 4) {
        JSONObject answer = new JSONObject();
        answer.put("index", answers.length() + 1);
        answer.put("answer", "Вариант " + (answers.length() + 1));
        answers.put(answer);
      }

      fixed.put("available_answers", answers);

      if (question.has("right_answer_number")) {
        int rightAnswer = question.getInt("right_answer_number");
        int maxAllowedAnswer = trueFalseMode ? 2 : 4;
        if (rightAnswer >= 1 && rightAnswer <= maxAllowedAnswer) {
          fixed.put("right_answer_number", rightAnswer);
        } else {
          fixed.put("right_answer_number", 1);
        }
      } else if (question.has("right_ans_index")) {
        int rightAnswer = question.getInt("right_ans_index");
        int maxAllowedAnswer = trueFalseMode ? 2 : 4;
        if (rightAnswer >= 1 && rightAnswer <= maxAllowedAnswer) {
          fixed.put("right_answer_number", rightAnswer);
        } else {
          fixed.put("right_answer_number", 1);
        }
      } else {
        fixed.put("right_answer_number", 1);
      }

      return fixed;

    } catch (Exception e) {
      System.err.println("Ошибка при исправлении структуры вопроса " + expectedNumber + ": " + e.getMessage());

      JSONObject minimalQuestion = new JSONObject();
      minimalQuestion.put("question_number", expectedNumber);
      minimalQuestion.put("question_text", "Вопрос на тему " + expectedNumber);

      JSONArray answers = new JSONArray();
      if (isTrueFalseMode(gameMode)) {
        answers.put(new JSONObject().put("index", 1).put("answer", "Правда"));
        answers.put(new JSONObject().put("index", 2).put("answer", "Ложь"));
        answers.put(new JSONObject().put("index", 3).put("answer", ""));
        answers.put(new JSONObject().put("index", 4).put("answer", ""));
      } else {
        for (int i = 1; i <= 4; i++) {
          JSONObject answer = new JSONObject();
          answer.put("index", i);
          answer.put("answer", "Вариант " + i);
          answers.put(answer);
        }
      }
      minimalQuestion.put("available_answers", answers);
      minimalQuestion.put("right_answer_number", 1);

      return minimalQuestion;
    }
  }

  private static String normalizeGameMode(String rawGameMode) {
    if (rawGameMode == null || rawGameMode.isBlank()) {
      return GAME_MODE_CASUAL;
    }
    String normalized = rawGameMode.trim().toUpperCase();
    return GAME_MODE_TRUE_FALSE.equals(normalized) ? GAME_MODE_TRUE_FALSE : GAME_MODE_CASUAL;
  }

  private static boolean isTrueFalseMode(String gameMode) {
    return GAME_MODE_TRUE_FALSE.equals(normalizeGameMode(gameMode));
  }
}
