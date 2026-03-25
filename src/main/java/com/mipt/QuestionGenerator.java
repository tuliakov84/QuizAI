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
import java.util.function.Supplier;

public class QuestionGenerator {

    private static final String URL = "http://localhost:11434/api/chat";

    private static final HttpClient CLIENT = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(30))
        .build();

    public static CompletableFuture<String> generate(String jsonString) {
        JSONObject obj = new JSONObject(jsonString);

        String topic = obj.getString("topic");
        int numberOfQuestions = obj.getInt("n");
        int difficult = obj.getInt("difficult");

        if (numberOfQuestions > 1) {
            return generateOneByOne(topic, numberOfQuestions, difficult);
        }

        return generateSingleQuestion(topic, difficult, 1);
    }

    private static CompletableFuture<String> generateSingleQuestion(String subtopic, int difficult, int questionNumber) {

        String prompt = String.format(
            "Сгенерируй 1 вопрос по теме '%s'.\n" +
            "Сложность: %d из 3.\n" +
            "Это вопрос №%d из серии.\n\n" +
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
            "4. Верни ТОЛЬКО ОДИН вопрос",
            subtopic, difficult, questionNumber, questionNumber
        );

        JSONObject payload = new JSONObject();
        payload.put("model", "qwen2.5");
        payload.put("format", "json");
        payload.put("stream", false);

        JSONObject options = new JSONObject();
        options.put("temperature", 0.3);
        options.put("num_predict", 2048);
        payload.put("options", options);

        JSONArray messages = new JSONArray();
        JSONObject m = new JSONObject();
        m.put("role", "user");
        m.put("content", prompt);
        messages.put(m);

        payload.put("messages", messages);

        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create(URL))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(payload.toString(), StandardCharsets.UTF_8))
            .build();

        return CLIENT.sendAsync(req, HttpResponse.BodyHandlers.ofString())
            .thenApply(response -> {
                if (response.statusCode() != 200) {
                    System.err.println("[QG] HTTP error: " + response.statusCode());
                    return null;
                }

                try {
                    JSONObject responseObj = new JSONObject(response.body());
                    String content = responseObj.getJSONObject("message").getString("content").trim();

                    if (content.isBlank()) return null;

                    String jsonContent = extractValidJson(content);
                    if (jsonContent == null) {
                        System.err.println("[QG] JSON не найден");
                        return null;
                    }

                    JSONObject questionObj = new JSONObject(jsonContent);
                    return fixQuestionStructure(questionObj, questionNumber).toString();

                } catch (Exception e) {
                    System.err.println("[QG] Ошибка парсинга: " + e.getMessage());
                    return null;
                }
            })
            .exceptionally(ex -> {
                System.err.println("[QG] Exception: " + ex.getMessage());
                return null;
            });
    }

    private static CompletableFuture<String> generateSubtopic(String topic, int difficult) {

        String prompt = String.format(
            "Придумай ОДНУ подтему для темы '%s' со сложностью %d.\n" +
            "Ответ ТОЛЬКО строкой.",
            topic, difficult
        );

        JSONObject payload = new JSONObject();
        payload.put("model", "qwen2.5");
        payload.put("stream", false);

        JSONObject options = new JSONObject();
        options.put("temperature", 0.7);
        options.put("num_predict", 100);
        payload.put("options", options);

        JSONArray messages = new JSONArray();
        JSONObject m = new JSONObject();
        m.put("role", "user");
        m.put("content", prompt);
        messages.put(m);

        payload.put("messages", messages);

        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create(URL))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(payload.toString(), StandardCharsets.UTF_8))
            .build();

        return CLIENT.sendAsync(req, HttpResponse.BodyHandlers.ofString())
            .thenApply(response -> {
                try {
                    JSONObject responseObj = new JSONObject(response.body());
                    return responseObj.getJSONObject("message").getString("content").trim();
                } catch (Exception e) {
                    System.err.println("[QG] Ошибка подтемы: " + e.getMessage());
                    return topic;
                }
            });
    }

    private static CompletableFuture<String> generateOneByOne(String topic, int n, int difficult) {

        return generateSubtopic(topic, difficult).thenCompose(subtopic -> {

            System.out.println("[QG] Подтема: " + subtopic);

            List<CompletableFuture<JSONObject>> futures = new ArrayList<>();

            for (int i = 1; i <= n; i++) {
                final int qn = i;

                futures.add(
                    withRetry(() -> generateSingleQuestion(subtopic, difficult, qn))
                        .thenApply(result -> {
                            if (result == null) return null;
                            try {
                                return new JSONObject(result);
                            } catch (Exception e) {
                                System.err.println("[QG] JSON ошибка: " + e.getMessage());
                                return null;
                            }
                        })
                );
            }

            return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> {
                    JSONArray arr = new JSONArray();

                    for (CompletableFuture<JSONObject> f : futures) {
                        JSONObject q = f.join();
                        if (q != null) arr.put(q);
                    }

                    return arr.toString();
                });
        });
    }

    private static String extractValidJson(String content) {
        if (content == null || !content.contains("{")) return null;

        int start = content.indexOf('{');
        int balance = 0;
        boolean inQuotes = false;
        char prev = 0;

        for (int i = start; i < content.length(); i++) {
            char c = content.charAt(i);

            if (c == '"' && prev != '\\') inQuotes = !inQuotes;
            else if (!inQuotes) {
                if (c == '{') balance++;
                else if (c == '}') balance--;
            }

            prev = c;

            if (balance == 0 && i > start) {
                return content.substring(start, i + 1);
            }
        }

        return null;
    }

    private static JSONObject fixQuestionStructure(JSONObject q, int num) {

        JSONObject res = new JSONObject();
        res.put("question_number", num);

        res.put("question_text",
            q.has("question_text") ? q.getString("question_text") :
            q.optString("question", "Вопрос")
        );

        JSONArray answers = new JSONArray();

        if (q.has("available_answers")) {
            JSONArray arr = q.getJSONArray("available_answers");

            for (int i = 0; i < Math.min(4, arr.length()); i++) {
                JSONObject a = arr.getJSONObject(i);

                JSONObject fixed = new JSONObject();
                fixed.put("index", i + 1);
                fixed.put("answer", a.optString("answer", "Вариант " + (i + 1)).replace("\"", ""));

                answers.put(fixed);
            }
        }

        while (answers.length() < 4) {
            JSONObject a = new JSONObject();
            a.put("index", answers.length() + 1);
            a.put("answer", "Вариант " + (answers.length() + 1));
            answers.put(a);
        }

        res.put("available_answers", answers);

        int right = q.optInt("right_answer_number", 1);
        if (right < 1 || right > 4) right = 1;

        res.put("right_answer_number", right);

        return res;
    }

    private static CompletableFuture<String> withRetry(Supplier<CompletableFuture<String>> supplier) {
        return supplier.get().thenCompose(result -> {
            if (result == null) {
                System.out.println("[QG] Retry...");
                return supplier.get();
            }
            return CompletableFuture.completedFuture(result);
        });
    }
}
