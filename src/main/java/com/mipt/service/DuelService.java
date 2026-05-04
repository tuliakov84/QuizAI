package com.mipt.service;

import com.mipt.QuestionGenerator;
import com.mipt.bank.CoinTransactionType;
import com.mipt.dbAPI.DatabaseAccessException;
import com.mipt.dbAPI.DbService;
import com.mipt.dbAPI.jpa.entity.UserEntity;
import com.mipt.domainModel.DuelActionRequest;
import com.mipt.domainModel.DuelStateResponse;
import com.mipt.domainModel.Question;
import com.mipt.domainModel.Topic;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class DuelService {
  private static final int ANTE = 10;
  private static final int BET_TIMEOUT_SECONDS = 20;
  private static final int WAIT_TIMEOUT_SECONDS = 120;
  private static final int BATCH_SIZE = 10;
  private static final int TOP_UP_THRESHOLD = 5;

  private final DbService dbService;
  private final Random random = new Random();
  private final Map<Integer, DuelMatchState> states = new ConcurrentHashMap<>();

  public DuelService(DbService dbService) {
    this.dbService = dbService;
  }

  public void ensureInitialBatchAsync(int gameId, int levelDifficulty, int topicId) {
    CompletableFuture.runAsync(() -> {
      try {
        System.out.println("[DUEL] Initial generation started for game " + gameId);
        ensureInitialBatch(gameId, levelDifficulty, topicId);
        System.out.println("[DUEL] Initial generation finished for game " + gameId);
      } catch (Exception e) {
        System.err.println("[DUEL] Initial async generation failed for game " + gameId + ": " + e.getMessage());
      }
    });
  }

  public void ensureInitialBatch(int gameId, int levelDifficulty, int topicId) throws SQLException, DatabaseAccessException {
    int count = (int) dbService.countQuestionsInGame(gameId);
    if (count >= BATCH_SIZE) {
      return;
    }
    try {
      List<UserEntity> participants = dbService.getParticipants(gameId);
      generateRandomQuestionsBatch(gameId, levelDifficulty, count + 1, BATCH_SIZE, participants, List.of());
    } catch (Exception e) {
      throw new DatabaseAccessException("Failed to generate initial duel questions", e);
    }
  }

  public void startDuel(int gameId) throws SQLException, DatabaseAccessException {
    DuelMatchState state = states.computeIfAbsent(gameId, id -> new DuelMatchState());
    synchronized (state) {
      System.out.println("[DUEL] startDuel called for game " + gameId);
      if (state.stage == Stage.FINISHED) {
        return;
      }

      List<UserEntity> participants = dbService.getParticipants(gameId);
      if (participants.size() != 2) {
        throw new DatabaseAccessException("Duel requires exactly 2 players in room");
      }

      if (state.players.isEmpty()) {
        for (UserEntity participant : participants) {
          PlayerState player = new PlayerState();
          player.userId = participant.getId();
          player.username = participant.getUsername();
          state.players.put(player.userId, player);
        }
      }

      Integer[] preset = dbService.getPreset(gameId);
      state.gameId = gameId;
      state.levelDifficulty = preset[1];
      state.topicId = preset[4];
      state.currentQuestionNumber = 1;
      state.pot = 0;
      state.currentBet = 0;
      state.allInMode = false;
      state.waitingDeadline = null;
      state.message = "Дуэль началась";

      payAnteFromBoth(state, "Initial duel ante");

      state.totalGeneratedQuestions = (int) dbService.countQuestionsInGame(gameId);
      openBettingStage(state, true);
    }
  }

  public DuelStateResponse getState(DuelActionRequest request) throws SQLException, DatabaseAccessException {
    int gameId = requireInt(request.getGameId(), "gameId");
    int userId = dbService.getUserId(requireString(request.getSession(), "session"));

    DuelMatchState state = states.computeIfAbsent(gameId, id -> new DuelMatchState());
    synchronized (state) {
      if (state.players.isEmpty()) {
        recoverStateFromDb(state, gameId);
      }
      if (!state.players.containsKey(userId)) {
        throw new DatabaseAccessException("User is not in this duel");
      }
      applyTimeouts(state);
      maybeResumeAfterWaiting(state);
      return toResponse(state, userId);
    }
  }

  public DuelStateResponse applyBetAction(DuelActionRequest request) throws SQLException, DatabaseAccessException {
    int gameId = requireInt(request.getGameId(), "gameId");
    String action = requireString(request.getAction(), "action").trim().toUpperCase(Locale.ROOT);
    int userId = dbService.getUserId(requireString(request.getSession(), "session"));

    DuelMatchState state = states.computeIfAbsent(gameId, id -> new DuelMatchState());
    synchronized (state) {
      if (state.players.isEmpty()) {
        recoverStateFromDb(state, gameId);
      }
      if (state.stage == Stage.FINISHED) {
        return toResponse(state, userId);
      }
      if (state.stage != Stage.BETTING) {
        throw new DatabaseAccessException("Betting stage is closed");
      }
      if (!Objects.equals(state.activePlayerId, userId)) {
        throw new DatabaseAccessException("It is not your turn");
      }

      applyTimeouts(state);
      if (state.stage != Stage.BETTING) {
        return toResponse(state, userId);
      }

      switch (action) {
        case "FOLD" -> finishWithWinner(state, getOpponentId(state, userId), "Оппонент сдался");
        case "CALL" -> processCall(state, userId);
        case "RAISE" -> processRaise(state, userId, request.getAmount());
        default -> throw new DatabaseAccessException("Unsupported action: " + action);
      }
      return toResponse(state, userId);
    }
  }

  public DuelStateResponse submitAnswer(DuelActionRequest request) throws SQLException, DatabaseAccessException {
    int gameId = requireInt(request.getGameId(), "gameId");
    int userId = dbService.getUserId(requireString(request.getSession(), "session"));
    DuelMatchState state = states.computeIfAbsent(gameId, id -> new DuelMatchState());

    synchronized (state) {
      if (state.players.isEmpty()) {
        recoverStateFromDb(state, gameId);
      }
      if (state.stage != Stage.ANSWERING) {
        throw new DatabaseAccessException("No active question to answer");
      }

      PlayerState player = state.players.get(userId);
      if (player == null) {
        throw new DatabaseAccessException("User is not in this duel");
      }
      if (player.answersByQuestion.containsKey(state.currentQuestionNumber)) {
        throw new DatabaseAccessException("Answer for this question already submitted");
      }

      Question question = dbService.getQuestionForAnyMode(gameId, state.currentQuestionNumber);
      boolean correct = isCorrectAnswer(question, request.getSubmittedAnswerNumber(), request.getSubmittedAnswerText());
      player.answersByQuestion.put(state.currentQuestionNumber, correct);

      boolean allAnswered = true;
      for (PlayerState participant : state.players.values()) {
        if (!participant.answersByQuestion.containsKey(state.currentQuestionNumber)) {
          allAnswered = false;
          break;
        }
      }

      if (allAnswered) {
        resolveRound(state);
      }

      return toResponse(state, userId);
    }
  }

  private void recoverStateFromDb(DuelMatchState state, int gameId) throws SQLException, DatabaseAccessException {
    List<UserEntity> participants = dbService.getParticipants(gameId);
    if (participants.size() != 2) {
      throw new DatabaseAccessException("Duel is not ready. Need exactly 2 players");
    }
    Integer[] preset = dbService.getPreset(gameId);
    state.gameId = gameId;
    state.levelDifficulty = preset[1];
    state.topicId = preset[4];
    if (state.currentQuestionNumber == 0) {
      state.currentQuestionNumber = 1;
    }
    for (UserEntity participant : participants) {
      state.players.computeIfAbsent(participant.getId(), id -> {
        PlayerState player = new PlayerState();
        player.userId = participant.getId();
        player.username = participant.getUsername();
        return player;
      });
    }
    state.totalGeneratedQuestions = (int) dbService.countQuestionsInGame(gameId);
  }

  private void processCall(DuelMatchState state, int userId) throws SQLException, DatabaseAccessException {
    PlayerState player = state.players.get(userId);
    int required = state.currentBet - player.committedBet;
    if (required > 0) {
      int available = dbService.getCoinBalanceByUserId(userId);
      int delta = Math.min(required, available);
      if (delta < required) {
        state.allInMode = true;
      }
      placeBet(state, userId, delta, "Call in duel");
    }

    if (allPlayersMatched(state)) {
      if (isAnyPlayerAllIn(state)) {
        state.allInMode = true;
      }
      openAnsweringStage(state);
    } else {
      switchTurn(state);
    }
  }

  private void processRaise(DuelMatchState state, int userId, Integer targetBetRaw) throws SQLException, DatabaseAccessException {
    if (targetBetRaw == null) {
      throw new DatabaseAccessException("amount is required for raise");
    }

    PlayerState player = state.players.get(userId);
    int available = dbService.getCoinBalanceByUserId(userId);
    int maxTargetBet = player.committedBet + available;
    int targetBet = Math.min(targetBetRaw, maxTargetBet);

    if (targetBet <= state.currentBet) {
      throw new DatabaseAccessException("Raise must exceed current bet");
    }

    int delta = targetBet - player.committedBet;
    placeBet(state, userId, delta, "Raise in duel");
    state.currentBet = targetBet;

    if (dbService.getCoinBalanceByUserId(userId) == 0) {
      state.allInMode = true;
    }

    switchTurn(state);
  }

  private void placeBet(DuelMatchState state, int userId, int amount, String reason) throws SQLException, DatabaseAccessException {
    if (amount <= 0) {
      return;
    }
    dbService.changeCoinBalanceByUserId(userId, -amount, CoinTransactionType.BET_PLACED, reason, state.gameId);
    PlayerState player = state.players.get(userId);
    player.committedBet += amount;
    state.pot += amount;
  }

  private void resolveRound(DuelMatchState state) throws SQLException, DatabaseAccessException {
    List<PlayerState> participants = new ArrayList<>(state.players.values());
    PlayerState first = participants.get(0);
    PlayerState second = participants.get(1);
    boolean firstCorrect = Boolean.TRUE.equals(first.answersByQuestion.get(state.currentQuestionNumber));
    boolean secondCorrect = Boolean.TRUE.equals(second.answersByQuestion.get(state.currentQuestionNumber));

    if (firstCorrect && !secondCorrect) {
      appendRoundResult(state, participants, firstCorrect, secondCorrect, "Победил первый игрок", state.pot, state.pot);
      finishWithWinner(state, first.userId, "Только один игрок ответил верно");
      return;
    }
    if (!firstCorrect && secondCorrect) {
      appendRoundResult(state, participants, firstCorrect, secondCorrect, "Победил второй игрок", state.pot, state.pot);
      finishWithWinner(state, second.userId, "Только один игрок ответил верно");
      return;
    }

    boolean antePlacedForRound = false;
    if (!firstCorrect) {
      state.pot = 0;
      resetRoundCommitments(state);
      appendRoundResult(state, participants, firstCorrect, secondCorrect, "Оба игрока ошиблись — банк сброшен и анте поставлено снова", 0, 0);
      payAnteFromBoth(state, "Rebuy ante after both wrong");
      antePlacedForRound = true;
      state.message = "Оба игрока ошиблись. Банк сброшен и анте поставлено снова";
    } else {
      resetRoundCommitments(state);
      state.message = "Оба игрока ответили верно. Дуэль продолжается";
      appendRoundResult(state, participants, firstCorrect, secondCorrect, "Оба игрока ответили верно", state.pot, 0);
    }

    state.currentQuestionNumber += 1;
    state.totalGeneratedQuestions = (int) dbService.countQuestionsInGame(state.gameId);

    ensureQuestionsForNextRounds(state);
    if (state.stage == Stage.WAITING_QUESTIONS || state.stage == Stage.FINISHED) {
      return;
    }

    openBettingStage(state, antePlacedForRound);
  }

  private void ensureQuestionsForNextRounds(DuelMatchState state) throws SQLException, DatabaseAccessException {
    state.totalGeneratedQuestions = (int) dbService.countQuestionsInGame(state.gameId);
    int remaining = state.totalGeneratedQuestions - state.currentQuestionNumber + 1;

    if (remaining <= TOP_UP_THRESHOLD && !state.generationInProgress) {
      state.generationInProgress = true;
      startTopUpGeneration(state);
    }

    if (state.currentQuestionNumber > state.totalGeneratedQuestions) {
      state.stage = Stage.WAITING_QUESTIONS;
      state.waitingDeadline = Instant.now().plusSeconds(WAIT_TIMEOUT_SECONDS);
      state.message = "Вопросы закончились. Ждем генерацию до 2 минут";
      if (!state.generationInProgress) {
        state.generationInProgress = true;
        startTopUpGeneration(state);
      }
    }
  }

  private void startTopUpGeneration(DuelMatchState state) {
    final int gameId = state.gameId;
    final int levelDifficulty = state.levelDifficulty;

    CompletableFuture.runAsync(() -> {
      try {
        System.out.println("[DUEL] Top-up generation started for game " + gameId);
        List<UserEntity> participants = dbService.getParticipants(gameId);
        int existing = (int) dbService.countQuestionsInGame(gameId);
        List<String> existingTexts = dbService.getQuestionTextsByGameIdExceptNumbers(gameId, List.of());
        generateRandomQuestionsBatch(gameId, levelDifficulty, existing + 1, BATCH_SIZE, participants, existingTexts);
        System.out.println("[DUEL] Top-up generation finished for game " + gameId);
      } catch (Exception e) {
        System.err.println("[DUEL] Top-up generation failed for game " + gameId + ": " + e.getMessage());
      } finally {
        DuelMatchState current = states.get(gameId);
        if (current != null) {
          synchronized (current) {
            current.generationInProgress = false;
            try {
              current.totalGeneratedQuestions = (int) dbService.countQuestionsInGame(gameId);
            } catch (Exception ignored) {
            }
          }
        }
      }
    });
  }

  private void generateQuestionsBatch(
      int gameId,
      String topic,
      int levelDifficulty,
      int startNumber,
      int amount,
      List<String> existingQuestions
  ) throws Exception {
    JSONArray payload = new JSONArray();
    JSONObject request = new JSONObject();
    request.put("topic", topic);
    request.put("numberOfQuestions", amount);
    request.put("difficult", levelDifficulty);
    request.put("gameMode", "CASUAL");

    JSONArray numbers = new JSONArray();
    for (int i = 0; i < amount; i++) {
      numbers.put(startNumber + i);
    }
    request.put("questionNumbersToRegenerate", numbers);

    JSONArray existing = new JSONArray();
    for (String question : existingQuestions) {
      existing.put(question);
    }
    request.put("existingQuestions", existing);
    payload.put(request);

    String generated = QuestionGenerator.generate(payload.toString()).join();
    JSONArray generatedArray = new JSONArray(generated);
    dbService.loadQuestions(gameId, generatedArray);
  }

  private void generateRandomQuestionsBatch(
      int gameId,
      int levelDifficulty,
      int startNumber,
      int amount,
      List<UserEntity> participants,
      List<String> existingQuestions
  ) throws Exception {
    Topic[] topics = dbService.getAllTopics();
    if (topics.length == 0) {
      throw new DatabaseAccessException("No topics available for duel generation");
    }

    List<String> generatedTexts = new ArrayList<>(existingQuestions == null ? List.<String>of() : existingQuestions);
    for (int i = 0; i < amount; i++) {
      Topic topic = topics[random.nextInt(topics.length)];
      String personalizedTopic = buildPersonalizedTopic(topic.getName(), participants, topic.getTopicId());
      System.out.println("[DUEL] Generating question " + (startNumber + i) + " with random topic '" + topic.getName() + "' for game " + gameId);
      JSONObject payloadItem = new JSONObject();
      payloadItem.put("topic", personalizedTopic);
      payloadItem.put("numberOfQuestions", 1);
      payloadItem.put("difficult", levelDifficulty);
      payloadItem.put("gameMode", "CASUAL");
      payloadItem.put("questionNumbersToRegenerate", new JSONArray().put(startNumber + i));
      payloadItem.put("existingQuestions", new JSONArray(generatedTexts));

      JSONArray payload = new JSONArray().put(payloadItem);
      String generated = QuestionGenerator.generate(payload.toString()).join();
      JSONArray generatedArray = new JSONArray(generated);
      if (generatedArray.isEmpty()) {
        continue;
      }
      JSONObject generatedQuestion = generatedArray.getJSONObject(0);
      String questionText = generatedQuestion.optString("question_text", "").trim();
      if (!questionText.isEmpty()) {
        generatedTexts.add(questionText);
      }
      dbService.loadQuestions(gameId, generatedArray);
    }
  }

  private String buildPersonalizedTopic(String topicName, List<UserEntity> participants, int topicId)
      throws SQLException, DatabaseAccessException {
    List<String> contexts = new ArrayList<>();

    for (UserEntity participant : participants) {
      List<String> lastQuestions = getRecentCorrectQuestionsForTopic(participant.getSession(), topicId, 10);
      if (!lastQuestions.isEmpty()) {
        contexts.add(participant.getUsername() + ": " + String.join("; ", lastQuestions));
      }
    }

    if (contexts.isEmpty()) {
      return topicName;
    }

    return topicName + ". Учти интересы игроков по их последним правильным ответам: "
        + String.join(" | ", contexts);
  }

  private List<String> getRecentCorrectQuestionsForTopic(String session, int topicId, int limit)
      throws SQLException, DatabaseAccessException {
    if (session == null || session.isBlank()) {
      return List.of();
    }

    Question[] answered = dbService.getCorrectAnswers(session);
    List<String> result = new ArrayList<>();

    for (int i = answered.length - 1; i >= 0; i--) {
      Question question = answered[i];
      if (question.getGameId() == null || question.getQuestionText() == null || question.getQuestionText().isBlank()) {
        continue;
      }
      Integer[] preset = dbService.getPreset(question.getGameId());
      if (preset[4] != null && preset[4] == topicId) {
        result.add(question.getQuestionText().trim());
      }
      if (result.size() >= limit) {
        break;
      }
    }

    return result;
  }

  private void payAnteFromBoth(DuelMatchState state, String reason) throws SQLException, DatabaseAccessException {
    boolean anyPlayerAllInAfterAnte = false;
    for (PlayerState player : state.players.values()) {
      int balance = dbService.getCoinBalanceByUserId(player.userId);
      int ante = Math.min(ANTE, balance);
      if (ante > 0) {
        placeBet(state, player.userId, ante, reason);
      }
      if (balance - ante == 0) {
        anyPlayerAllInAfterAnte = true;
      }
    }

    int maxCommitted = 0;
    for (PlayerState player : state.players.values()) {
      if (player.committedBet > maxCommitted) {
        maxCommitted = player.committedBet;
      }
    }
    state.currentBet = maxCommitted;
    state.allInMode = state.allInMode || anyPlayerAllInAfterAnte;
  }

  private void openBettingStage(DuelMatchState state, boolean fromStart) throws SQLException, DatabaseAccessException {
    if (state.stage == Stage.FINISHED) {
      return;
    }

    if (state.currentQuestionNumber > state.totalGeneratedQuestions) {
      ensureQuestionsForNextRounds(state);
      if (state.stage == Stage.WAITING_QUESTIONS) {
        return;
      }
    }

    if (state.allInMode) {
      openAnsweringStage(state);
      return;
    }

    if (!fromStart) {
      resetRoundCommitments(state);
      state.currentBet = 0;
    }

    state.stage = Stage.BETTING;
    state.activePlayerId = pickRandomPlayerId(state);
    state.actionDeadline = Instant.now().plusSeconds(BET_TIMEOUT_SECONDS);
    state.message = fromStart ? "Ставка анте принята. Ход первого игрока" : "Новый раунд ставок";
  }

  private void openAnsweringStage(DuelMatchState state) throws SQLException, DatabaseAccessException {
    state.stage = Stage.ANSWERING;
    state.activePlayerId = null;
    state.actionDeadline = null;
    state.message = "Оба игрока могут отвечать на вопрос";

    Question question = dbService.getQuestionForAnyMode(state.gameId, state.currentQuestionNumber);
    state.currentQuestion = question;
  }

  private void appendRoundResult(
      DuelMatchState state,
      List<PlayerState> participants,
      boolean firstCorrect,
      boolean secondCorrect,
      String outcome,
      int pot,
      int payout
  ) throws SQLException, DatabaseAccessException {
    if (participants.size() < 2) {
      return;
    }

    Question question = dbService.getQuestionForAnyMode(state.gameId, state.currentQuestionNumber);
    DuelStateResponse.DuelRoundResultView round = new DuelStateResponse.DuelRoundResultView();
    round.setRoundNumber(state.roundResults.size() + 1);
    round.setQuestionNumber(state.currentQuestionNumber);
    round.setQuestionText(question.getQuestionText());
    round.setOutcome(outcome);
    round.setFirstPlayerName(participants.get(0).username);
    round.setFirstPlayerCorrect(firstCorrect);
    round.setSecondPlayerName(participants.get(1).username);
    round.setSecondPlayerCorrect(secondCorrect);
    round.setPot(pot);
    round.setPayout(payout);
    state.roundResults.add(round);
  }

  private void resetRoundCommitments(DuelMatchState state) {
    for (PlayerState player : state.players.values()) {
      player.committedBet = 0;
    }
  }

  private void applyTimeouts(DuelMatchState state) throws SQLException, DatabaseAccessException {
    if (state.stage == Stage.BETTING && state.actionDeadline != null && Instant.now().isAfter(state.actionDeadline)) {
      if (state.activePlayerId != null) {
        int winnerId = getOpponentId(state, state.activePlayerId);
        finishWithWinner(state, winnerId, "Оппонент не сделал ставку за 20 секунд");
      }
    }

    if (state.stage == Stage.WAITING_QUESTIONS && state.waitingDeadline != null && Instant.now().isAfter(state.waitingDeadline)) {
      finishWithDraw(state, "Вопросы не успели сгенерироваться. Ничья");
    }
  }

  private void maybeResumeAfterWaiting(DuelMatchState state) throws SQLException, DatabaseAccessException {
    if (state.stage != Stage.WAITING_QUESTIONS) {
      return;
    }
    state.totalGeneratedQuestions = (int) dbService.countQuestionsInGame(state.gameId);
    if (state.currentQuestionNumber <= state.totalGeneratedQuestions) {
      state.waitingDeadline = null;
      openBettingStage(state, false);
    }
  }

  private void finishWithWinner(DuelMatchState state, int winnerId, String text) throws SQLException, DatabaseAccessException {
    if (state.stage == Stage.FINISHED) {
      return;
    }

    PlayerState winner = state.players.get(winnerId);
    PlayerState loser = state.players.get(getOpponentId(state, winnerId));

    int payout = state.pot;
    if (payout > 0) {
      dbService.changeCoinBalanceByUserId(winnerId, payout, CoinTransactionType.GAME_PAYOUT, "Duel payout", state.gameId);
    }

    DuelStateResponse.DuelResultView result = new DuelStateResponse.DuelResultView();
    result.setDraw(false);
    result.setWinnerUserId(winner.userId);
    result.setWinnerUsername(winner.username);
    result.setLoserUserId(loser.userId);
    result.setLoserUsername(loser.username);
    result.setPayout(payout);
    result.setText(text);

    state.result = result;
    state.message = text;
    state.stage = Stage.FINISHED;
    state.activePlayerId = null;
    state.actionDeadline = null;
    state.waitingDeadline = null;

    dbService.stopGame(state.gameId);
  }

  private void finishWithDraw(DuelMatchState state, String text) throws SQLException, DatabaseAccessException {
    if (state.stage == Stage.FINISHED) {
      return;
    }

    DuelStateResponse.DuelResultView result = new DuelStateResponse.DuelResultView();
    result.setDraw(true);
    result.setPayout(0);
    result.setText(text);

    state.result = result;
    state.message = text;
    state.stage = Stage.FINISHED;
    state.activePlayerId = null;
    state.actionDeadline = null;
    state.waitingDeadline = null;

    dbService.stopGame(state.gameId);
  }

  private DuelStateResponse toResponse(DuelMatchState state, int currentUserId) throws SQLException, DatabaseAccessException {
    DuelStateResponse response = new DuelStateResponse();
    response.setGameId(state.gameId);
    response.setStage(state.stage.name());
    response.setMessage(state.message);
    response.setPot(state.pot);
    response.setCurrentBet(state.currentBet);
    response.setCurrentQuestionNumber(state.currentQuestionNumber);
    response.setTotalGeneratedQuestions(state.totalGeneratedQuestions);
    response.setAllInMode(state.allInMode);
    response.setMyTurn(Objects.equals(state.activePlayerId, currentUserId));
    response.setActivePlayerId(state.activePlayerId);
    response.setActionDeadlineEpochMs(state.actionDeadline == null ? null : state.actionDeadline.toEpochMilli());
    response.setWaitingDeadlineEpochMs(state.waitingDeadline == null ? null : state.waitingDeadline.toEpochMilli());
    response.setQuestion(state.stage == Stage.ANSWERING ? state.currentQuestion : null);
    response.setRoundResults(new ArrayList<>(state.roundResults));
    response.setResult(state.result);

    List<DuelStateResponse.DuelPlayerView> players = new ArrayList<>();
    for (PlayerState player : state.players.values()) {
      DuelStateResponse.DuelPlayerView view = new DuelStateResponse.DuelPlayerView();
      view.setUserId(player.userId);
      view.setUsername(player.username);
      view.setCoinBalance(dbService.getCoinBalanceByUserId(player.userId));
      view.setCommittedBet(player.committedBet);
      view.setAnswered(player.answersByQuestion.containsKey(state.currentQuestionNumber));
      players.add(view);
    }
    response.setPlayers(players);

    return response;
  }

  private boolean isCorrectAnswer(Question question, Integer answerNumber, String answerText) {
    if (answerNumber != null && answerNumber > 0) {
      return Objects.equals(answerNumber, question.getRightAnswerNumber());
    }

    if (answerText == null || answerText.isBlank()) {
      return false;
    }

    String rightAnswer = switch (question.getRightAnswerNumber()) {
      case 1 -> question.getAnswer1();
      case 2 -> question.getAnswer2();
      case 3 -> question.getAnswer3();
      case 4 -> question.getAnswer4();
      default -> "";
    };

    return isTextSimilar(answerText, rightAnswer);
  }

  private boolean isTextSimilar(String submitted, String right) {
    String s = normalize(submitted);
    String r = normalize(right);

    if (s.isEmpty() || r.isEmpty()) {
      return false;
    }
    if (s.equals(r)) {
      return true;
    }
    if (s.length() > 3 && (r.contains(s) || s.contains(r))) {
      return true;
    }

    String[] sTokens = s.split(" ");
    String[] rTokens = r.split(" ");
    int intersection = 0;
    for (String sToken : sTokens) {
      for (String rToken : rTokens) {
        if (sToken.equals(rToken)) {
          intersection++;
          break;
        }
      }
    }

    double jaccard = (double) intersection / (sTokens.length + rTokens.length - intersection);
    return jaccard >= 0.6;
  }

  private String normalize(String value) {
    if (value == null) {
      return "";
    }
    return value.toLowerCase(Locale.ROOT)
        .replaceAll("[\\p{Punct}]", " ")
        .replaceAll("\\s+", " ")
        .trim();
  }

  private boolean allPlayersMatched(DuelMatchState state) {
    for (PlayerState player : state.players.values()) {
      if (player.committedBet < state.currentBet) {
        return false;
      }
    }
    return true;
  }

  private boolean isAnyPlayerAllIn(DuelMatchState state) throws SQLException, DatabaseAccessException {
    for (PlayerState player : state.players.values()) {
      if (dbService.getCoinBalanceByUserId(player.userId) == 0) {
        return true;
      }
    }
    return false;
  }

  private void switchTurn(DuelMatchState state) {
    if (state.activePlayerId == null) {
      return;
    }
    state.activePlayerId = getOpponentId(state, state.activePlayerId);
    state.actionDeadline = Instant.now().plusSeconds(BET_TIMEOUT_SECONDS);
  }

  private int pickRandomPlayerId(DuelMatchState state) {
    List<Integer> ids = new ArrayList<>(state.players.keySet());
    return ids.get(random.nextInt(ids.size()));
  }

  private int getOpponentId(DuelMatchState state, int userId) {
    for (Integer id : state.players.keySet()) {
      if (id != userId) {
        return id;
      }
    }
    return userId;
  }

  private int requireInt(Integer value, String fieldName) throws DatabaseAccessException {
    if (value == null) {
      throw new DatabaseAccessException("Field '" + fieldName + "' is required");
    }
    return value;
  }

  private String requireString(String value, String fieldName) throws DatabaseAccessException {
    if (value == null || value.isBlank()) {
      throw new DatabaseAccessException("Field '" + fieldName + "' is required");
    }
    return value;
  }

  private enum Stage {
    BETTING,
    ANSWERING,
    WAITING_QUESTIONS,
    FINISHED
  }

  private static class DuelMatchState {
    int gameId;
    int topicId;
    int levelDifficulty;
    int currentQuestionNumber;
    int totalGeneratedQuestions;
    int pot;
    int currentBet;
    boolean allInMode;
    boolean generationInProgress;
    Integer activePlayerId;
    Instant actionDeadline;
    Instant waitingDeadline;
    String message;
    Question currentQuestion;
    Stage stage = Stage.BETTING;
    DuelStateResponse.DuelResultView result;
    List<DuelStateResponse.DuelRoundResultView> roundResults = new ArrayList<>();
    Map<Integer, PlayerState> players = new LinkedHashMap<>();
  }

  private static class PlayerState {
    int userId;
    String username;
    int committedBet;
    Map<Integer, Boolean> answersByQuestion = new ConcurrentHashMap<>();
  }
}












