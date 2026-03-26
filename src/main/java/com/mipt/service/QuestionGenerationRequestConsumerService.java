package com.mipt.service;

import com.mipt.QuestionGenerator;
import com.mipt.dbAPI.DbService;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "app.service.role", havingValue = "generator")
public class QuestionGenerationRequestConsumerService {

  private static final Logger LOGGER = LoggerFactory.getLogger(QuestionGenerationRequestConsumerService.class);

  private final DbService dbService;
  private final KafkaTemplate<String, String> kafkaTemplate;
  @Value("${app.kafka.topic.question-generation-results}")
  private String generationResultsTopic;

  public QuestionGenerationRequestConsumerService(DbService dbService, KafkaTemplate<String, String> kafkaTemplate) {
    this.dbService = dbService;
    this.kafkaTemplate = kafkaTemplate;
  }

  @KafkaListener(topics = "${app.kafka.topic.question-generation-requests}", groupId = "${spring.kafka.consumer.group-id}")
  public void consumeQuestionGenerationRequest(String message) {
    try {
      JSONObject request = new JSONObject(message);
      int gameId = request.getInt("gameId");
      int topicId = request.getInt("topicId");
      int levelDifficulty = request.getInt("levelDifficulty");
      int numberOfQuestions = request.getInt("numberOfQuestions");

      String topicName = dbService.getTopicById(topicId).getName();
      String generatorPayload = buildGeneratorPayload(topicName, numberOfQuestions, levelDifficulty);
      String rawQuestions = QuestionGenerator.generate(generatorPayload).join();

      JSONObject response = new JSONObject(request.toString());
      response.put("status", "GENERATED");
      response.put("questions", new JSONArray(rawQuestions));

      kafkaTemplate.send(
          generationResultsTopic,
          String.valueOf(gameId),
          response.toString()
      );
      LOGGER.info("Generated questions sent for validation for gameId={}, requestId={}", gameId, response.optString("requestId", ""));
    } catch (Exception e) {
      LOGGER.error("Failed to handle generation request message: {}", message, e);
    }
  }

  private static String buildGeneratorPayload(String topicName, int numberOfQuestions, int levelDifficulty) {
    return "[{\"topic\":\"" + topicName + "\",\"numberOfQuestions\":" + numberOfQuestions
        + ",\"difficult\":" + levelDifficulty + "}]";
  }
}
