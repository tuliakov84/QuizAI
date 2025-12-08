package com.mipt;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

public class QuestionGenerator {

    // Хранилище уже сгенерированных вопросов для предотвращения повторов
    private static final Set<String> generatedQuestions = new HashSet<>();
    private static final Object lock = new Object();

    public static CompletableFuture<String> generate(String jsonString) {
        JSONArray inputArray = new JSONArray(jsonString);
        JSONObject obj = inputArray.getJSONObject(0);

        String topic = obj.getString("topic");
        int numberOfQuestions = obj.getInt("numberOfQuestions");
        int difficult = obj.getInt("difficult");

        // Очищаем хранилище при каждом новом вызове
        synchronized (lock) {
            generatedQuestions.clear();
        }

        if (numberOfQuestions > 1) {
            return generateSequentially(topic, numberOfQuestions, difficult);
        }

        return generateSingleQuestion(topic, difficult, 1, "");
    }

    private static CompletableFuture<String> generateSingleQuestion(String topic, int difficult,
                                                                    int questionNumber, String previousQuestions) {
        String url = "http://localhost:11434/api/chat";

        // Добавляем контекст о предыдущих вопросах, чтобы избежать повторов
        String contextPrompt = previousQuestions.isEmpty() ?
            "Пока нет сгенерированных вопросов." :
            "Уже сгенерированы следующие вопросы:\n" + previousQuestions;

        String prompt = String.format(
            "Сгенерируй УНИКАЛЬНЫЙ вопрос на тему '%s'. Сложность: %d (1-легко, 3-сложно).\n\n" +
                "%s\n\n" +
                "ВАЖНО: Вопрос должен быть уникальным и не повторять уже сгенерированные выше.\n\n" +
                "Формат ответа ТОЛЬКО JSON-объект следующей структуры:\n" +
                "{\n" +
                "  \"question_number\": %d,\n" +
                "  \"question_text\": \"Текст вопроса на русском языке\",\n" +
                "  \"available_answers\": [\n" +
                "    {\"index\": 1, \"answer\": \"Вариант ответа 1\"},\n" +
                "    {\"index\": 2, \"answer\": \"Вариант ответа 2\"},\n" +
                "    {\"index\": 3, \"answer\": \"Вариант ответа 3\"},\n" +
                "    {\"index\": 4, \"answer\": \"Вариант ответа 4\"}\n" +
                "  ],\n" +
                "  \"right_answer_number\": номер_правильного_ответа_от_1_до_4\n" +
                "}\n\n" +
                "Требования к вопросу:\n" +
                "1. Все на русском языке\n" +
                "2. Только JSON, без других слов\n" +
                "3. Правильный JSON синтаксис\n" +
                "4. Вопрос должен быть УНИКАЛЬНЫМ и не повторять предыдущие\n" +
                "5. Варианты ответов должны быть разнообразными и релевантными",
            topic, difficult, contextPrompt, questionNumber
        );

        JSONObject payload = new JSONObject();
        payload.put("model", "qwen2.5");
        payload.put("format", "json");
        payload.put("stream", false);

        JSONObject options = new JSONObject();
        options.put("temperature", 0.7); // Немного повышаем температуру для разнообразия
        options.put("num_predict", 2048);
        payload.put("options", options);

        JSONArray messages = new JSONArray();
        JSONObject m = new JSONObject();
        m.put("role", "user");
        m.put("content", prompt);
        messages.put(m);

        payload.put("messages", messages);

        HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(payload.toString(), StandardCharsets.UTF_8))
            .build();

        return client.sendAsync(req, HttpResponse.BodyHandlers.ofString())
            .thenApply(response -> {
                if (response.statusCode() != 200) {
                    System.err.println("Ошибка API: " + response.statusCode());
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

                    // Проверяем уникальность вопроса
                    String questionText = questionObj.optString("question_text",
                        questionObj.optString("question", ""));

                    synchronized (lock) {
                        // Если вопрос уже был сгенерирован, возвращаем null для повторной генерации
                        if (!questionText.isEmpty() && generatedQuestions.contains(questionText.toLowerCase())) {
                            System.err.println("Обнаружен повторный вопрос: " + questionText.substring(0, Math.min(50, questionText.length())));
                            return null;
                        }
                        generatedQuestions.add(questionText.toLowerCase());
                    }

                    JSONObject fixedQuestion = fixQuestionStructure(questionObj, questionNumber);
                    return fixedQuestion;

                } catch (Exception e) {
                    System.err.println("Ошибка при генерации вопроса " + questionNumber + ": " + e.getMessage());
                    return null;
                }
            })
            .exceptionally(ex -> {
                System.err.println("Исключение при запросе вопроса " + questionNumber + ": " + ex.getMessage());
                return null;
            })
            .thenApply(questionObj -> {
                if (questionObj == null) {
                    return "null";
                }
                return questionObj.toString();
            });
    }

    private static CompletableFuture<String> generateSequentially(String topic, int numberOfQuestions, int difficult) {
        AtomicInteger generatedCount = new AtomicInteger(0);
        JSONArray allQuestions = new JSONArray();
        StringBuilder previousQuestions = new StringBuilder();

        // Вместо параллельной генерации делаем последовательную с учетом предыдущих вопросов
        return generateQuestionRecursively(topic, difficult, numberOfQuestions, 1,
            generatedCount, allQuestions, previousQuestions, 0);
    }

    private static CompletableFuture<String> generateQuestionRecursively(String topic, int difficult,
                                                                         int totalQuestions, int currentQuestion,
                                                                         AtomicInteger generatedCount,
                                                                         JSONArray allQuestions,
                                                                         StringBuilder previousQuestions,
                                                                         int retryCount) {
        if (currentQuestion > totalQuestions || retryCount > 3) { // Максимум 3 попытки на вопрос
            JSONObject result = new JSONObject();
            result.put("generated_count", generatedCount.get());
            result.put("questions", allQuestions);

            System.out.println("Сгенерировано вопросов: " + generatedCount.get() + " из " + totalQuestions);

            if (generatedCount.get() < totalQuestions) {
                System.out.println("Предупреждение: получено " + generatedCount.get() +
                    " уникальных вопросов вместо " + totalQuestions);
            }

            return CompletableFuture.completedFuture(allQuestions.toString());
        }

        return generateSingleQuestion(topic, difficult, currentQuestion, previousQuestions.toString())
            .thenCompose(result -> {
                if (result.equals("null")) {
                    // Если не удалось сгенерировать, пробуем еще раз
                    System.err.println("Попытка " + (retryCount + 1) + " для вопроса " + currentQuestion + " не удалась");
                    return generateQuestionRecursively(topic, difficult, totalQuestions, currentQuestion,
                        generatedCount, allQuestions, previousQuestions, retryCount + 1);
                }

                try {
                    JSONObject question = new JSONObject(result);

                    // Добавляем вопрос в список
                    allQuestions.put(question);
                    generatedCount.incrementAndGet();

                    // Обновляем контекст для следующих вопросов
                    String questionText = question.getString("question_text");
                    previousQuestions.append(currentQuestion).append(". ").append(questionText).append("\n");

                    // Переходим к следующему вопросу
                    return generateQuestionRecursively(topic, difficult, totalQuestions, currentQuestion + 1,
                        generatedCount, allQuestions, previousQuestions, 0);

                } catch (Exception e) {
                    System.err.println("Ошибка при обработке вопроса " + currentQuestion + ": " + e.getMessage());
                    return generateQuestionRecursively(topic, difficult, totalQuestions, currentQuestion,
                        generatedCount, allQuestions, previousQuestions, retryCount + 1);
                }
            });
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

    private static JSONObject fixQuestionStructure(JSONObject question, int expectedNumber) {
        try {
            JSONObject fixed = new JSONObject();
            fixed.put("question_number", expectedNumber);

            if (question.has("question_text")) {
                fixed.put("question_text", question.getString("question_text"));
            } else if (question.has("question")) {
                fixed.put("question_text", question.getString("question"));
            } else {
                fixed.put("question_text", "Вопрос на тему " + expectedNumber);
            }

            JSONArray answers = new JSONArray();
            if (question.has("available_answers")) {
                JSONArray originalAnswers = question.getJSONArray("available_answers");
                for (int i = 0; i < Math.min(4, originalAnswers.length()); i++) {
                    JSONObject answerObj = originalAnswers.getJSONObject(i);
                    JSONObject fixedAnswer = new JSONObject();

                    if (answerObj.has("index")) {
                        fixedAnswer.put("index", answerObj.getInt("index"));
                    } else {
                        fixedAnswer.put("index", i + 1);
                    }

                    if (answerObj.has("answer")) {
                        String answerText = answerObj.getString("answer");
                        answerText = answerText.replaceAll("\"Впитер\"", "Юпитер")
                            .replaceAll("\"Ипитер\"", "Юпитер")
                            .replaceAll("\"\\\\\"", "\"") // Убираем экранированные кавычки
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
                if (rightAnswer >= 1 && rightAnswer <= 4) {
                    fixed.put("right_answer_number", rightAnswer);
                } else {
                    fixed.put("right_answer_number", 1);
                }
            } else if (question.has("right_ans_index")) {
                int rightAnswer = question.getInt("right_ans_index");
                if (rightAnswer >= 1 && rightAnswer <= 4) {
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
            for (int i = 1; i <= 4; i++) {
                JSONObject answer = new JSONObject();
                answer.put("index", i);
                answer.put("answer", "Вариант " + i);
                answers.put(answer);
            }
            minimalQuestion.put("available_answers", answers);
            minimalQuestion.put("right_answer_number", 1);

            return minimalQuestion;
        }
    }
}