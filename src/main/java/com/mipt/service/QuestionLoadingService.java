package com.mipt.service;

import com.mipt.domainModel.Game;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class QuestionLoadingService {

  private static final Logger LOGGER = LoggerFactory.getLogger(QuestionLoadingService.class);

  private final MlQuestionRequestProducerService mlQuestionRequestProducer;

  public QuestionLoadingService(MlQuestionRequestProducerService mlQuestionRequestProducer) {
    this.mlQuestionRequestProducer = mlQuestionRequestProducer;
  }

  /**
   * Отправляет в Kafka запрос на генерацию вопросов ML/LLM для игры.
   * Консьюмер сгенерирует вопросы и сохранит их в БД.
   */
  @Async
  public void loadQuestionsAsync(int gameId, int levelDifficulty, int numberOfQuestions, int topicId) {
    try {
      Game game = new Game();
      game.setGameId(gameId);
      game.setTopicId(topicId);
      game.setNumberOfQuestions(numberOfQuestions);
      game.setLevelDifficulty(levelDifficulty);
      mlQuestionRequestProducer.sendQuestionGenerationRequest(game);
    } catch (Exception e) {
      LOGGER.error("Failed to send question generation request for game {}", gameId, e);
    }
  }
}

