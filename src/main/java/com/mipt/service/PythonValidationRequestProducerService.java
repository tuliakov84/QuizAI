package com.mipt.service;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PythonValidationRequestProducerService {

  private static final Logger LOGGER = LoggerFactory.getLogger(PythonValidationRequestProducerService.class);

  private final KafkaTemplate<String, String> kafkaTemplate;

  @Value("${app.kafka.topic.python-validation-requests}")
  private String topicName;

  public PythonValidationRequestProducerService(KafkaTemplate<String, String> kafkaTemplate) {
    this.kafkaTemplate = kafkaTemplate;
  }

  public void sendValidationRequest(int gameId, String requestId, int attempt, List<Integer> questionIds) {
    JSONObject payload = new JSONObject();
    payload.put("status", "VALIDATION_REQUESTED");
    payload.put("gameId", gameId);
    payload.put("requestId", requestId);
    payload.put("attempt", attempt);
    payload.put("questionIds", new JSONArray(questionIds));

    kafkaTemplate.send(topicName, String.valueOf(gameId), payload.toString());
    LOGGER.info(
        "Sent python validation request for gameId={}, requestId={}, attempt={}, questionCount={}",
        gameId,
        requestId,
        attempt,
        questionIds.size()
    );
  }
}
