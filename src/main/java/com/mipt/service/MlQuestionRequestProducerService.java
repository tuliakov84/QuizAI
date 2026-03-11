package com.mipt.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mipt.domainModel.Game;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/**
 * Sends question generation requests to Kafka for the ML/LLM pipeline.
 * Messages contain the Game with everything needed for the LLM to generate questions.
 */
@Service
public class MlQuestionRequestProducerService {

  private static final Logger LOGGER = LoggerFactory.getLogger(MlQuestionRequestProducerService.class);

  private final KafkaTemplate<String, String> kafkaTemplate;
  private final String topicName;
  private final ObjectMapper objectMapper;

  public MlQuestionRequestProducerService(
      KafkaTemplate<String, String> kafkaTemplate,
      @Value("${app.kafka.topic.question-generation-requests}") String topicName,
      ObjectMapper objectMapper) {
    this.kafkaTemplate = kafkaTemplate;
    this.topicName = topicName;
    this.objectMapper = objectMapper;
  }

  /**
   * Sends the game to Kafka for the ML/LLM to generate questions. The consumer will
   * generate questions and persist them to the DB for this game.
   */
  public void sendQuestionGenerationRequest(Game game) {
    try {
      String payload = objectMapper.writeValueAsString(game);
      kafkaTemplate.send(topicName, String.valueOf(game.getGameId()), payload);
      LOGGER.info("Sent question generation request for gameId={} topicId={} count={}",
          game.getGameId(), game.getTopicId(), game.getNumberOfQuestions());
    } catch (JsonProcessingException e) {
      LOGGER.error("Failed to serialize game for question generation, gameId={}", game.getGameId(), e);
      throw new RuntimeException("Failed to send question generation request", e);
    }
  }
}
