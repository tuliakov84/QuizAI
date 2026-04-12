package com.mipt.service;

import com.mipt.dbAPI.DatabaseAccessException;
import com.mipt.dbAPI.DbService;
import com.mipt.domainModel.Topic;
import com.mipt.utils.QuestionPayloadFinalNormalizer;
import org.json.JSONArray;
import org.json.JSONObject;
import com.mipt.domainModel.Game;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Service
public class QuestionLoadingService {

  private static final Logger LOGGER = LoggerFactory.getLogger(QuestionLoadingService.class);
  private static final int MAX_PERSONALIZATION_ATTEMPTS = 3;
  private static final int CANDIDATE_MULTIPLIER = 2;
  private static final int EXTRA_CANDIDATES = 3;

  private final MlQuestionRequestProducerService mlQuestionRequestProducer;
  private final DbService dbService;
  /** false — синхронная загрузка (в т.ч. для unit-тестов без Kafka) */
  private final boolean useKafkaForLoad;

  @Autowired
  public QuestionLoadingService(MlQuestionRequestProducerService mlQuestionRequestProducer, DbService dbService) {
    this(mlQuestionRequestProducer, dbService, true);
  }

  /**
   * @param useKafkaForLoad если {@code false}, вопросы собираются локально (персонализация + БД), без Kafka.
   */
  public QuestionLoadingService(
      MlQuestionRequestProducerService mlQuestionRequestProducer,
      DbService dbService,
      boolean useKafkaForLoad
  ) {
    this.mlQuestionRequestProducer = mlQuestionRequestProducer;
    this.dbService = dbService;
    this.useKafkaForLoad = useKafkaForLoad;
  }

  /**
   * Отправляет в Kafka запрос на генерацию вопросов ML/LLM для игры.
   * Консьюмер сгенерирует вопросы и сохранит их в БД.
   */
  @Async
  public void loadQuestionsAsync(int gameId, int levelDifficulty, int numberOfQuestions, int topicId) {
    try {
      sendGenerationRequest(gameId, levelDifficulty, numberOfQuestions, topicId);
    } catch (Exception e) {
      LOGGER.error("Failed to send generation request for game {}", gameId, e);
    }
  }

  public void loadQuestions(int gameId, int levelDifficulty, int numberOfQuestions, int topicId)
      throws SQLException, DatabaseAccessException {
    if (useKafkaForLoad) {
      sendGenerationRequest(gameId, levelDifficulty, numberOfQuestions, topicId);
    } else {
      loadQuestionsInline(gameId, levelDifficulty, numberOfQuestions, topicId);
    }
  }

  private void loadQuestionsInline(int gameId, int levelDifficulty, int numberOfQuestions, int topicId)
      throws SQLException, DatabaseAccessException {
    Topic topic = dbService.getTopicById(topicId);
    JSONArray selectedQuestions = buildPersonalizedQuestionSet(
        gameId,
        topic.getName(),
        levelDifficulty,
        numberOfQuestions
    );
    List<Integer> questionIds = dbService.loadQuestions(gameId, selectedQuestions);
    persistEmbeddings(questionIds);
  }

  private void sendGenerationRequest(int gameId, int levelDifficulty, int numberOfQuestions, int topicId)
      throws SQLException, DatabaseAccessException {
    Topic topic = dbService.getTopicById(topicId);
    Game game = new Game();
    game.setGameId(gameId);
    game.setTopicId(topic.getTopicId());
    game.setLevelDifficulty(levelDifficulty);
    game.setNumberOfQuestions(numberOfQuestions);
    mlQuestionRequestProducer.sendQuestionGenerationRequest(game);
  }

  private JSONArray buildPersonalizedQuestionSet(
      int gameId,
      String topicName,
      int levelDifficulty,
      int targetQuestionCount
  ) {
    JSONArray selectedQuestions = new JSONArray();

    for (int attempt = 0; attempt < MAX_PERSONALIZATION_ATTEMPTS; attempt++) {
      int remainingQuestions = targetQuestionCount - selectedQuestions.length();
      if (remainingQuestions <= 0) {
        break;
      }

      int candidateCount = calculateCandidateCount(remainingQuestions);
      JSONArray rawCandidates = generateCandidateQuestions(topicName, candidateCount, levelDifficulty);

      JSONObject personalizationResult = personalizeCandidates(
          gameId,
          rawCandidates,
          remainingQuestions,
          selectedQuestions
      );
      JSONArray selectedBatch = personalizationResult.optJSONArray("selected_questions");
      if (selectedBatch == null || selectedBatch.length() == 0) {
        LOGGER.warn("Personalization attempt {} for game {} produced no usable questions", attempt + 1, gameId);
        continue;
      }

      for (int i = 0; i < selectedBatch.length(); i++) {
        JSONObject finalizedQuestion = QuestionPayloadFinalNormalizer.normalizeQuestion(selectedBatch.getJSONObject(i));
        if (QuestionPayloadFinalNormalizer.containsChineseText(finalizedQuestion)) {
          LOGGER.warn(
              "Dropping question with Chinese text after personalization. gameId={}, attempt={}, questionNumber={}",
              gameId,
              attempt + 1,
              finalizedQuestion.optInt("question_number", -1)
          );
          continue;
        }

        finalizedQuestion.put("question_number", selectedQuestions.length() + 1);
        selectedQuestions.put(finalizedQuestion);
      }
    }

    if (selectedQuestions.length() < targetQuestionCount) {
      throw new IllegalStateException(
          "Failed to assemble enough personalized questions for game " + gameId
              + ": got " + selectedQuestions.length() + " of " + targetQuestionCount
      );
    }

    return selectedQuestions;
  }

  protected JSONArray generateCandidateQuestions(String topicName, int candidateCount, int levelDifficulty) {
    return new JSONArray();
  }

  protected JSONObject personalizeCandidates(
      int gameId,
      JSONArray candidateQuestions,
      int targetCount,
      JSONArray existingQuestions
  ) {
    try {
      Path candidatesFile = writeTempJson("quizai-candidates-", candidateQuestions);
      Path existingQuestionsFile = writeTempJson("quizai-existing-", existingQuestions);

      List<String> command = new ArrayList<>();
      command.add("python3");
      command.add(pythonScriptPath("service", "question_personalization.py").toString());
      command.add("--game-id");
      command.add(String.valueOf(gameId));
      command.add("--candidates-file");
      command.add(candidatesFile.toString());
      command.add("--target-count");
      command.add(String.valueOf(targetCount));
      command.add("--existing-questions-file");
      command.add(existingQuestionsFile.toString());

      ProcessResult result = runCommand(command);
      return new JSONObject(result.stdout());
    } catch (Exception e) {
      throw new IllegalStateException("Failed to personalize generated questions for game " + gameId, e);
    }
  }

  protected void persistEmbeddings(List<Integer> questionIds) {
    if (questionIds.isEmpty()) {
      return;
    }

    try {
      JSONArray questionIdsJson = new JSONArray(questionIds);
      Path questionIdsFile = writeTempJson("quizai-question-ids-", questionIdsJson);

      List<String> command = List.of(
          "python3",
          pythonScriptPath("service", "generate_embeddings_for_questions.py").toString(),
          "--question-ids-file",
          questionIdsFile.toString()
      );

      runCommand(command);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to persist question embeddings", e);
    }
  }

  private Path writeTempJson(String prefix, JSONArray payload) throws IOException {
    Path tempFile = Files.createTempFile(prefix, ".json");
    Files.writeString(tempFile, payload.toString(), StandardCharsets.UTF_8);
    tempFile.toFile().deleteOnExit();
    return tempFile;
  }

  private ProcessResult runCommand(List<String> command) throws IOException, InterruptedException {
    ProcessBuilder processBuilder = new ProcessBuilder(command);
    processBuilder.directory(projectRoot().toFile());

    Process process = processBuilder.start();
    String stdout = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
    String stderr = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8).trim();
    int exitCode = process.waitFor();

    if (exitCode != 0) {
      throw new IllegalStateException("Command failed: " + String.join(" ", command)
          + "\nstdout: " + stdout + "\nstderr: " + stderr);
    }

    if (stdout.isEmpty()) {
      throw new IllegalStateException("Command returned no output: " + String.join(" ", command));
    }

    return new ProcessResult(stdout, stderr);
  }

  private int calculateCandidateCount(int remainingQuestions) {
    return Math.max(remainingQuestions * CANDIDATE_MULTIPLIER, remainingQuestions + EXTRA_CANDIDATES);
  }

  private Path pythonScriptPath(String... pathParts) {
    Path result = projectRoot().resolve(Path.of("src", "main", "python"));
    for (String pathPart : pathParts) {
      result = result.resolve(pathPart);
    }
    return result;
  }

  private Path projectRoot() {
    return Path.of(System.getProperty("user.dir"));
  }

  private record ProcessResult(String stdout, String stderr) {
  }
}
