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
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class QuestionGenerator {

    public static CompletableFuture<String> generate(String jsonString) {
        JSONArray inputArray = new JSONArray(jsonString);
        JSONObject obj = inputArray.getJSONObject(0);

        String topic = obj.getString("topic");
        int numberOfQuestions = obj.getInt("numberOfQuestions");
        int difficult = obj.getInt("difficult");

        if (numberOfQuestions > 1) {
            return generateOneByOne(topic, numberOfQuestions, difficult);
        }

        return generateSingleQuestion(topic, difficult, 1);
    }

    private static CompletableFuture<String> generateSingleQuestion(String topic, int difficult, int questionNumber) {
        String url = "http://localhost:11434/api/chat";

        String prompt = String.format(
            "Сгенерируй 1 вопрос на тему '%s'. Сложность: %d (1-легко, 3-сложно).\n\n" +
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
                "Важно:\n" +
                "1. Все на русском языке\n" +
                "2. Только JSON, без других слов\n" +
                "3. Правильный JSON синтаксис\n" +
                "4. Верни ТОЛЬКО ОДИН вопрос в формате объекта",
            topic, difficult, questionNumber
        );

        JSONObject payload = new JSONObject();
        payload.put("model", "qwen2.5");
        payload.put("format", "json");
        payload.put("stream", false);

        JSONObject options = new JSONObject();
        options.put("temperature", 0.0);
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

                    // Извлекаем и исправляем JSON
                    String jsonContent = extractValidJson(content);
                    if (jsonContent == null) {
                        System.err.println("Не удалось извлечь JSON для вопроса " + questionNumber);
                        return null;
                    }

                    // Парсим как объект
                    JSONObject questionObj = new JSONObject(jsonContent);

                    // Исправляем структуру вопроса
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

    private static CompletableFuture<String> generateOneByOne(String topic, int numberOfQuestions, int difficult) {
        List<CompletableFuture<JSONObject>> futures = new ArrayList<>();

        // Генерируем все вопросы параллельно
        for (int i = 1; i <= numberOfQuestions; i++) {
            final int questionNumber = i;
            CompletableFuture<JSONObject> future = generateSingleQuestion(topic, difficult, questionNumber)
                .thenApply(result -> {
                    if (result.equals("null")) {
                        return null;
                    }
                    try {
                        return new JSONObject(result);
                    } catch (Exception e) {
                        System.err.println("Ошибка парсинга вопроса " + questionNumber + ": " + e.getMessage());
                        return null;
                    }
                });
            futures.add(future);
        }

        // Ждем завершения всех futures
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> {
                JSONArray questions = new JSONArray();
                int generatedCount = 0;

                for (int i = 0; i < futures.size(); i++) {
                    try {
                        JSONObject question = futures.get(i).get();
                        if (question != null) {
                            questions.put(question);
                            generatedCount++;
                        }
                    } catch (Exception e) {
                        System.err.println("Ошибка при получении вопроса " + (i + 1) + ": " + e.getMessage());
                    }
                }

                System.out.println("Сгенерировано вопросов: " + generatedCount + " из " + numberOfQuestions);

                if (generatedCount < numberOfQuestions) {
                    System.out.println("Предупреждение: получено " + generatedCount + " вопросов вместо " + numberOfQuestions);
                }

                return questions.toString();
            });
    }

    private static String extractValidJson(String content) {
        if (content == null || content.isEmpty()) {
            return null;
        }

        content = content.trim();

        // Ищем начало объекта
        int start = content.indexOf('{');
        if (start == -1) {
            return null;
        }

        // Ищем конец объекта
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

            // Устанавливаем номер вопроса
            fixed.put("question_number", expectedNumber);

            // Устанавливаем текст вопроса
            if (question.has("question_text")) {
                fixed.put("question_text", question.getString("question_text"));
            } else if (question.has("question")) {
                fixed.put("question_text", question.getString("question"));
            } else {
                fixed.put("question_text", "Вопрос на тему");
            }

            // Обрабатываем варианты ответов
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
                        // Исправляем ошибки в тексте
                        answerText = answerText.replaceAll("\"Впитер\"", "Юпитер")
                            .replaceAll("\"Ипитер\"", "Юпитер");
                        fixedAnswer.put("answer", answerText);
                    } else {
                        fixedAnswer.put("answer", "Вариант " + (i + 1));
                    }

                    answers.put(fixedAnswer);
                }
            }

            // Заполняем до 4 ответов если меньше
            while (answers.length() < 4) {
                JSONObject answer = new JSONObject();
                answer.put("index", answers.length() + 1);
                answer.put("answer", "Вариант " + (answers.length() + 1));
                answers.put(answer);
            }

            fixed.put("available_answers", answers);

            // Устанавливаем правильный ответ
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
            System.err.println("Ошибка при исправлении структуры вопроса: " + e.getMessage());

            // Создаем минимально валидный вопрос
            JSONObject minimalQuestion = new JSONObject();
            minimalQuestion.put("question_number", expectedNumber);
            minimalQuestion.put("question_text", "Вопрос на тему");

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
