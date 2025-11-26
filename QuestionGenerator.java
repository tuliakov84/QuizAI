package org.example;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
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
                "You are a quiz generator AI.\n" +
                        "\n" +
                        "Your task:\n" +
                        "Generate %d multiple-choice questions on the topic \"%s\" with difficulty %d.\n" +
                        "All output MUST be fully in Russian.\n" +
                        "Each question must have exactly 4 answer choices and must indicate the correct one.\n" +
                        "Difficulty level is an integer from 1 to 3 (1 = easy, 2 = medium, 3 = hard). You must generate questions that match the difficulty level.\n" +
                        "\n" +
                        "Below is the JSON template for a single question:\n" +
                        "\n" +
                        "{\n" +
                        "  \"number\": <k + 1>,\n" +
                        "  \"question\": \"<question text in Russian>\",\n" +
                        "  \"available_answers\":\n" +
                        "  [\n" +
                        "    {\n" +
                        "      \"index\": 1,\n" +
                        "      \"answer\": \"<answer1>\"\n" +
                        "    },\n" +
                        "    {\n" +
                        "      \"index\": 2,\n" +
                        "      \"answer\": \"<answer2>\"\n" +
                        "    },\n" +
                        "    {\n" +
                        "      \"index\": 3,\n" +
                        "      \"answer\": \"<answer3>\"\n" +
                        "    },\n" +
                        "    {\n" +
                        "      \"index\": 4,\n" +
                        "      \"answer\": \"<answer4>\"\n" +
                        "    }\n" +
                        "  ],\n" +
                        "  \"right_ans_index\": <correct answer index>\n" +
                        "}\n" +
                        "\n" +
                        "FINAL OUTPUT FORMAT (strict):\n" +
                        "[\n" +
                        "  {\n" +
                        "    \"questions\":\n" +
                        "    [\n" +
                        "      ... %d question objects following the template above ...\n" +
                        "    ]\n" +
                        "  }\n" +
                        "]\n" +
                        "\n" +
                        "Rules:\n" +
                        "1. The final output must be ONLY valid JSON.\n" +
                        "2. No explanations, comments, markdown, or any text outside JSON.\n" +
                        "3. The top-level JSON must be an array (outer square brackets).\n" +
                        "4. The array must contain exactly one object, which has the key \"questions\".\n" +
                        "5. The \"questions\" value must be an array of exactly %d question objects.\n" +
                        "6. The field \"number\" must go from 1 to %d.\n" +
                        "7. All question texts and answers must be strictly in Russian.\n" +
                        "8. Questions must be unique and not repeat phrasing/answers.\n" +
                        "9. ALL indentation, spaces, line breaks, and JSON structure MUST strictly follow the provided template.\n" +
                        "   Formatting is absolutely fixed:\n" +
                        "   - Each level of nesting must use exactly two spaces for indentation.\n" +
                        "   - Every opening curly brace and opening square bracket MUST appear on a new line.\n" +
                        "   - Every closing curly brace and closing square bracket MUST also appear on a new line.\n" +
                        "   - Opening and closing braces/brackets may NEVER appear on the same line as other content.\n" +
                        "   - Arrays and objects must always begin with an opening bracket/brace on one line and end with a closing bracket/brace on its own separate line.\n" +
                        "   - Every field must start on a new line.\n" +
                        "   - A line break must follow every comma.\n" +
                        "   - No extra spaces, no missing spaces, no empty lines, no comments, no markdown, and no text outside of the JSON are allowed.\n" +
                        "   Any deviation from the structural or visual formatting of the template is strictly forbidden.\n",
                n, topic, difficult, n, n, n
        );


        JSONObject payload = new JSONObject();
        payload.put("model", "qwen2.5");
        payload.put("format", "json");
        payload.put("stream", false);

        JSONArray messages = new JSONArray();
        JSONObject m = new JSONObject();
        m.put("role", "user");
        m.put("content", prompt);
        messages.put(m);

        payload.put("messages", messages);

        HttpClient client = HttpClient.newHttpClient();

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString(), StandardCharsets.UTF_8))
                .build();

        return client.sendAsync(req, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenApply(response -> {

                    // извлечь только message.content
                    JSONObject obj2 = new JSONObject(response);
                    JSONObject messageObj = obj2.getJSONObject("message");

                    return messageObj.getString("content"); // ← чистый JSON
                });
    }
}