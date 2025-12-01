package com.mipt;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public class QuestionGenerator {

    public static CompletableFuture<String> generate(String jsonString) {
        JSONArray inputArray = new JSONArray(jsonString);
        JSONObject obj = inputArray.getJSONObject(0);

        String topic = obj.getString("topic");
        int n = obj.getInt("n");
        int difficult = obj.getInt("difficult");

        String url = "http://localhost:11434/api/chat";

        String prompt = String.format(
            "Сгенерируй %d вопросов на тему '%s'. Сложность: %d (1-легко, 3-сложно).\n\n" +
                "Формат ответа ТОЛЬКО JSON:\n" +
                "{\n" +
                "  \"questions\": [\n" +
                "    {\n" +
                "      \"number\": 1,\n" +
                "      \"question\": \"Вопрос на русском языке\",\n" +
                "      \"available_answers\": [\n" +
                "        {\"index\": 1, \"answer\": \"Ответ 1\"},\n" +
                "        {\"index\": 2, \"answer\": \"Ответ 2\"},\n" +
                "        {\"index\": 3, \"answer\": \"Ответ 3\"},\n" +
                "        {\"index\": 4, \"answer\": \"Ответ 4\"}\n" +
                "      ],\n" +
                "      \"right_ans_index\": 1\n" +
                "    }\n" +
                "  ]\n" +
                "}\n\n" +
                "Важно:\n" +
                "1. Все на русском языке\n" +
                "2. Только JSON, без других слов\n" +
                "3. Правильный JSON синтаксис\n" +
                "4. ОБЯЗАТЕЛЬНО сгенерируй РОВНО %d вопросов",
            n, topic, difficult, n
        );

        JSONObject payload = new JSONObject();
        payload.put("model", "qwen2.5");
        payload.put("format", "json");
        payload.put("stream", false);

        JSONObject options = new JSONObject();
        options.put("temperature", 0.3);
        options.put("num_predict", 8192); // Увеличиваем для больших ответов
        payload.put("options", options);

        JSONArray messages = new JSONArray();
        JSONObject m = new JSONObject();
        m.put("role", "user");
        m.put("content", prompt);
        messages.put(m);

        payload.put("messages", messages);

        HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(60)) // Увеличиваем таймаут подключения
            .build();

        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Content-Type", "application/json")
            // УБИРАЕМ .timeout() - запрос будет выполняться неограниченное время
            .POST(HttpRequest.BodyPublishers.ofString(payload.toString(), StandardCharsets.UTF_8))
            .build();

        return client.sendAsync(req, HttpResponse.BodyHandlers.ofString())
            .thenApply(response -> {
                System.out.println("Статус: " + response.statusCode());

                if (response.statusCode() != 200) {
                    System.err.println("Ошибка API: " + response.body());
                    return "{\"questions\": []}";
                }

                try {
                    JSONObject responseObj = new JSONObject(response.body());
                    JSONObject messageObj = responseObj.getJSONObject("message");
                    String content = messageObj.getString("content").trim();

                    System.out.println("Сырой ответ модели (первые 500 символов):");
                    System.out.println(content.substring(0, Math.min(500, content.length())));
                    if (content.length() > 500) {
                        System.out.println("... [всего символов: " + content.length() + "]");
                    }

                    // Проверяем, не обрезан ли ответ
                    if (isJsonTruncated(content)) {
                        System.err.println("Предупреждение: JSON может быть обрезан");
                        content = fixTruncatedJson(content);
                    }

                    // Очищаем и исправляем JSON
                    String cleanedJson = cleanAndFixJson(content);

                    if (cleanedJson == null || cleanedJson.isEmpty()) {
                        System.err.println("Не удалось обработать JSON");
                        return "{\"questions\": []}";
                    }

                    // Проверяем валидность
                    JSONObject result = new JSONObject(cleanedJson);

                    // Проверяем структуру
                    if (!result.has("questions")) {
                        System.err.println("Нет поля 'questions' в JSON");
                        return "{\"questions\": []}";
                    }

                    JSONArray questions = result.getJSONArray("questions");
                    System.out.println("Успешно! Получено вопросов: " + questions.length());

                    // Проверяем количество вопросов
                    if (questions.length() < n) {
                        System.out.println("Предупреждение: получено " + questions.length() +
                            " вопросов вместо " + n);
                    }

                    return removeDuplicateKeys(cleanedJson);

                } catch (Exception e) {
                    System.err.println("Ошибка обработки: " + e.getMessage());
                    e.printStackTrace();

                    return "{\"questions\": []}";
                }
            })
            .exceptionally(ex -> {
                System.err.println("Исключение при запросе: " + ex.getMessage());
                if (ex.getCause() != null) {
                    System.err.println("Причина: " + ex.getCause().getMessage());
                }
                return "{\"questions\": []}";
            });
    }

    private static boolean isJsonTruncated(String json) {
        if (json == null || json.trim().isEmpty()) {
            return false;
        }

        String trimmed = json.trim();

        if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) {
            return true;
        }

        // Считаем баланс скобок
        int braceBalance = 0;
        int bracketBalance = 0;
        boolean inQuotes = false;
        char prevChar = 0;

        for (char c : trimmed.toCharArray()) {
            if (c == '"' && prevChar != '\\') {
                inQuotes = !inQuotes;
            } else if (!inQuotes) {
                if (c == '{') braceBalance++;
                else if (c == '}') braceBalance--;
                else if (c == '[') bracketBalance++;
                else if (c == ']') bracketBalance--;
            }
            prevChar = c;
        }

        return braceBalance != 0 || bracketBalance != 0;
    }

    private static String fixTruncatedJson(String json) {
        if (json == null || json.trim().isEmpty()) {
            return json;
        }

        String trimmed = json.trim();
        StringBuilder fixed = new StringBuilder(trimmed);

        // Считаем баланс скобок
        int braceBalance = 0;
        int bracketBalance = 0;
        boolean inQuotes = false;
        char prevChar = 0;

        for (char c : trimmed.toCharArray()) {
            if (c == '"' && prevChar != '\\') {
                inQuotes = !inQuotes;
            } else if (!inQuotes) {
                if (c == '{') braceBalance++;
                else if (c == '}') braceBalance--;
                else if (c == '[') bracketBalance++;
                else if (c == ']') bracketBalance--;
            }
            prevChar = c;
        }

        // Закрываем незакрытые скобки
        for (int i = 0; i < bracketBalance; i++) {
            fixed.append("]");
        }

        for (int i = 0; i < braceBalance; i++) {
            fixed.append("}");
        }

        if (!fixed.toString().endsWith("}")) {
            fixed.append("}");
        }

        return fixed.toString();
    }

    private static String cleanAndFixJson(String rawJson) {
        if (rawJson == null || rawJson.trim().isEmpty()) {
            return null;
        }

        String json = rawJson.trim();

        // Удаляем все до первой '{' и после последней '}'
        int start = json.indexOf('{');
        int end = json.lastIndexOf('}');

        if (start == -1 || end == -1 || end <= start) {
            return null;
        }

        json = json.substring(start, end + 1);

        // Исправляем распространенные ошибки
        json = json.replaceAll("\"questions\":\\s*\\[\\[", "\"questions\": [")
            .replaceAll("\\]\\]\\s*\\}", "] }")
            .replaceAll(";", ",")
            .replaceAll("\"answer\":\\s*\"([^\"]*?)\\]\"", "\"answer\":\"$1\"")
            .replaceAll("\"answer\":\\s*\"([^\"]*?);\"", "\"answer\":\"$1\"")
            .replaceAll("\"answer\":\\s*\"([^\"]*?)\\}\"", "\"answer\":\"$1\"")
            .replaceAll("\"answer\":\\s*([^\"\\[{}\\],]+)(?=[,\\]}])", "\"answer\":\"$1\"")
            .replaceAll(",\\s*]", "]")
            .replaceAll(",\\s*}", "}");

        return json;
    }

    private static String removeDuplicateKeys(String jsonString) {
        try {
            JSONObject json = new JSONObject(jsonString);

            if (json.has("questions")) {
                JSONArray questions = json.getJSONArray("questions");
                JSONObject cleanJson = new JSONObject();
                cleanJson.put("questions", questions);
                return cleanJson.toString();
            }

            return jsonString;
        } catch (Exception e) {
            return jsonString;
        }
    }
}