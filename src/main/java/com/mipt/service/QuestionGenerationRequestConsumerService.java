package com.mipt.service;

import com.mipt.QuestionGenerator;
import com.mipt.gameModes.GameMode;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Service
@ConditionalOnExpression(
    "'${app.service.role:backend}' == 'generator' || '${app.service.role:backend}' == 'both'"
)
public class QuestionGenerationRequestConsumerService {

  private static final Logger LOGGER = LoggerFactory.getLogger(QuestionGenerationRequestConsumerService.class);
  private static final String TRUE_FALSE_MOCK_QUESTIONS_RESOURCE = "true-false-mock-questions.json";

  private final KafkaTemplate<String, String> kafkaTemplate;
  @Value("${app.kafka.topic.question-generation-results}")
  private String generationResultsTopic;
  @Value("${app.question-generation.mock-enabled:false}")
  private boolean mockGenerationEnabled;
  @Value("${app.question-generation.mock-resource:ml-answer-example.json}")
  private String mockQuestionsResource;

  public QuestionGenerationRequestConsumerService(KafkaTemplate<String, String> kafkaTemplate) {
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
      GameMode gameMode = parseGameMode(request.optString("gameMode", GameMode.CASUAL.name()));
      LOGGER.info(
          "Step A1: generator received request. gameId={}, topicId={}, difficulty={}, requestedCount={}, gameMode={}, requestId={}, attempt={}",
          gameId, topicId, levelDifficulty, numberOfQuestions, gameMode, request.optString("requestId", ""), request.optInt("attempt", 0)
      );
      JSONArray questionNumbersToRegenerate = request.optJSONArray("questionNumbersToRegenerate");
      JSONArray existingQuestions = request.optJSONArray("existingQuestions");
      if (existingQuestions == null) {
        existingQuestions = new JSONArray();
      }
      boolean regenerationRequested = request.optBoolean("isRegeneration", false);
      if (questionNumbersToRegenerate != null && !questionNumbersToRegenerate.isEmpty()) {
        numberOfQuestions = questionNumbersToRegenerate.length();
        regenerationRequested = true;
        LOGGER.info(
            "Step A2: regeneration mode. gameId={}, numbersToRegenerate={}, effectiveCount={}, existingQuestionsCount={}",
            gameId, questionNumbersToRegenerate, numberOfQuestions, existingQuestions.length()
        );
      }

      String topicName = request.optString("topicName", "").trim();
      if (topicName.isEmpty()) {
        throw new IllegalArgumentException("Field 'topicName' is required in generation request payload");
      }
      JSONArray generatedQuestions;
      if (mockGenerationEnabled) {
        String mockResource = resolveMockResource(gameMode);
        LOGGER.info(
            "Step A3: mock question generation enabled. gameId={}, topicName={}, payloadCount={}, gameMode={}, resource={}",
            gameId, topicName, numberOfQuestions, gameMode, mockResource
        );
        generatedQuestions = loadMockQuestions(mockResource, numberOfQuestions);
      } else {
        String generatorPayload = buildGeneratorPayload(
            topicName,
            numberOfQuestions,
            levelDifficulty,
            gameMode,
            regenerationRequested,
            questionNumbersToRegenerate,
            existingQuestions
        );
        LOGGER.info(
            "Step A3: calling QuestionGenerator. gameId={}, topicName={}, payloadCount={}, gameMode={}",
            gameId,
            topicName,
            numberOfQuestions,
            gameMode
        );
        generatedQuestions = new JSONArray(QuestionGenerator.generate(generatorPayload).join());
      }
      LOGGER.info("Step A4: generator returned {} questions for gameId={}", generatedQuestions.length(), gameId);
      if (questionNumbersToRegenerate != null && !questionNumbersToRegenerate.isEmpty()) {
        alignQuestionNumbers(generatedQuestions, questionNumbersToRegenerate);
        LOGGER.info("Step A5: aligned regenerated question numbers for gameId={}", gameId);
      }

      JSONObject response = new JSONObject(request.toString());
      response.put("status", "GENERATED");
      response.put("questions", generatedQuestions);

      kafkaTemplate.send(
          generationResultsTopic,
          String.valueOf(gameId),
          response.toString()
      );
      LOGGER.info(
          "Step A6: sent GENERATED payload to backend. gameId={}, requestId={}, generatedCount={}",
          gameId, response.optString("requestId", ""), generatedQuestions.length()
      );
    } catch (Exception e) {
      LOGGER.error("Failed to handle generation request message: {}", message, e);
    }
  }

  private static String buildGeneratorPayload(
      String topicName,
      int numberOfQuestions,
      int levelDifficulty,
      GameMode gameMode,
      boolean isRegeneration,
      JSONArray questionNumbersToRegenerate,
      JSONArray existingQuestions
  ) {
    JSONObject payloadObject = new JSONObject();
    payloadObject.put("topic", topicName);
    payloadObject.put("numberOfQuestions", numberOfQuestions);
    payloadObject.put("difficult", levelDifficulty);
    payloadObject.put("gameMode", gameMode.name());
    payloadObject.put("isRegeneration", isRegeneration);
    if (questionNumbersToRegenerate != null && !questionNumbersToRegenerate.isEmpty()) {
      payloadObject.put("questionNumbersToRegenerate", questionNumbersToRegenerate);
    }
    if (existingQuestions != null && !existingQuestions.isEmpty()) {
      payloadObject.put("existingQuestions", existingQuestions);
    }
    return new JSONArray().put(payloadObject).toString();
  }


  private JSONArray loadMockQuestions(String resourceName, int numberOfQuestions) throws Exception {
    InputStream inputStream = QuestionGenerationRequestConsumerService.class.getClassLoader()
        .getResourceAsStream(resourceName);
    if (inputStream == null) {
      throw new IllegalStateException("Mock questions resource not found: " + resourceName);
    }

    JSONArray sourceQuestions = new JSONArray(new String(inputStream.readAllBytes(), StandardCharsets.UTF_8));
    if (sourceQuestions.isEmpty()) {
      throw new IllegalStateException("Mock questions resource is empty: " + resourceName);
    }

    JSONArray result = new JSONArray();
    for (int i = 0; i < numberOfQuestions; i++) {
      JSONObject question = new JSONObject(sourceQuestions.getJSONObject(i % sourceQuestions.length()).toString());
      question.put("question_number", i + 1);
      if (i >= sourceQuestions.length()) {
        question.put("question_text", question.getString("question_text") + " #" + (i + 1));
      }
      result.put(question);
    }
    return result;
  }

  private static GameMode parseGameMode(String rawGameMode) {
    try {
      return GameMode.valueOf(rawGameMode == null ? GameMode.CASUAL.name() : rawGameMode.trim().toUpperCase());
    } catch (IllegalArgumentException e) {
      return GameMode.CASUAL;
    }
  }

  private String resolveMockResource(GameMode gameMode) {
    return GameMode.TRUE_FALSE.equals(gameMode)
        ? TRUE_FALSE_MOCK_QUESTIONS_RESOURCE
        : mockQuestionsResource;
  }

  private static void alignQuestionNumbers(JSONArray questions, JSONArray targetQuestionNumbers) {
    if (questions.length() != targetQuestionNumbers.length()) {
      throw new IllegalArgumentException(
          "Mismatch between generated questions and target question numbers: generated="
              + questions.length() + ", target=" + targetQuestionNumbers.length()
      );
    }
    for (int i = 0; i < questions.length(); i++) {
      questions.getJSONObject(i).put("question_number", targetQuestionNumbers.getInt(i));
    }
  }
}
