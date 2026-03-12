package com.mipt.service;

import com.mipt.domainModel.Game;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class MlQuestionRequestProducerService {

  private static final Logger LOGGER = LoggerFactory.getLogger(MlQuestionRequestProducerService.class);

  private final KafkaTemplate<String, String> kafkaTemplate;

  @Value("${app.kafka.topic.ml-question-requests}")
  private String topicName;

  public MlQuestionRequestProducerService(KafkaTemplate<String, String> kafkaTemplate) {
    this.kafkaTemplate = kafkaTemplate;
  }

  /**
   * Отправляет в Kafka сообщение с данными игры для генерации вопросов LLM.
   * Консьюмер использует эти данные для вызова ML-сервиса и сохранения результатов в БД.
   */
  public void sendQuestionGenerationRequest(Game game) {
    String payload = toJson(game);
    int gameId = game.getGameId();
    try {
      kafkaTemplate.send(topicName, String.valueOf(gameId), payload);
      LOGGER.info("Sent ML question request for gameId={}, topicId={}", gameId, game.getTopicId());
    } catch (Exception e) {
      LOGGER.error("Failed to send ML question request for gameId={}", gameId, e);
      throw new RuntimeException("Failed to send question generation request to Kafka", e);
    }
  }

  private static String toJson(Game game) {
    JSONObject obj = new JSONObject();
    obj.put("gameId", game.getGameId());
    obj.put("topicId", game.getTopicId());
    obj.put("numberOfQuestions", game.getNumberOfQuestions());
    obj.put("levelDifficulty", game.getLevelDifficultyInt());
    return obj.toString();
  }
}
