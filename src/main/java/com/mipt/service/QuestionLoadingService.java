package com.mipt.service;

import com.mipt.dbAPI.DbService;
import com.mipt.domainModel.Game;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class QuestionLoadingService {

  private static final Logger LOGGER = LoggerFactory.getLogger(QuestionLoadingService.class);

  private final DbService dbService;
  private final MlQuestionRequestProducerService mlQuestionRequestProducer;

  public QuestionLoadingService(DbService dbService,
      MlQuestionRequestProducerService mlQuestionRequestProducer) {
    this.dbService = dbService;
    this.mlQuestionRequestProducer = mlQuestionRequestProducer;
  }

  /**
   * Sends a question generation request to Kafka for the ML/LLM pipeline.
   * Builds a Game with the given parameters and sends it. The consumer will generate
   * questions and persist them to the DB for this game.
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

