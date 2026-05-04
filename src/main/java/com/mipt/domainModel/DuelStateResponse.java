package com.mipt.domainModel;

import java.util.ArrayList;
import java.util.List;

public class DuelStateResponse {
  private Integer gameId;
  private String stage;
  private String message;
  private Integer pot;
  private Integer currentBet;
  private Integer currentQuestionNumber;
  private Integer totalGeneratedQuestions;
  private Boolean allInMode;
  private Boolean myTurn;
  private Long actionDeadlineEpochMs;
  private Long waitingDeadlineEpochMs;
  private Integer activePlayerId;
  private Question question;
  private List<DuelPlayerView> players = new ArrayList<>();
  private List<DuelRoundResultView> roundResults = new ArrayList<>();
  private DuelResultView result;

  public Integer getGameId() {
    return gameId;
  }

  public void setGameId(Integer gameId) {
    this.gameId = gameId;
  }

  public String getStage() {
    return stage;
  }

  public void setStage(String stage) {
    this.stage = stage;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public Integer getPot() {
    return pot;
  }

  public void setPot(Integer pot) {
    this.pot = pot;
  }

  public Integer getCurrentBet() {
    return currentBet;
  }

  public void setCurrentBet(Integer currentBet) {
    this.currentBet = currentBet;
  }

  public Integer getCurrentQuestionNumber() {
    return currentQuestionNumber;
  }

  public void setCurrentQuestionNumber(Integer currentQuestionNumber) {
    this.currentQuestionNumber = currentQuestionNumber;
  }

  public Integer getTotalGeneratedQuestions() {
    return totalGeneratedQuestions;
  }

  public void setTotalGeneratedQuestions(Integer totalGeneratedQuestions) {
    this.totalGeneratedQuestions = totalGeneratedQuestions;
  }

  public Boolean getAllInMode() {
    return allInMode;
  }

  public void setAllInMode(Boolean allInMode) {
    this.allInMode = allInMode;
  }

  public Boolean getMyTurn() {
    return myTurn;
  }

  public void setMyTurn(Boolean myTurn) {
    this.myTurn = myTurn;
  }

  public Long getActionDeadlineEpochMs() {
    return actionDeadlineEpochMs;
  }

  public void setActionDeadlineEpochMs(Long actionDeadlineEpochMs) {
    this.actionDeadlineEpochMs = actionDeadlineEpochMs;
  }

  public Long getWaitingDeadlineEpochMs() {
    return waitingDeadlineEpochMs;
  }

  public void setWaitingDeadlineEpochMs(Long waitingDeadlineEpochMs) {
    this.waitingDeadlineEpochMs = waitingDeadlineEpochMs;
  }

  public Integer getActivePlayerId() {
    return activePlayerId;
  }

  public void setActivePlayerId(Integer activePlayerId) {
    this.activePlayerId = activePlayerId;
  }

  public Question getQuestion() {
    return question;
  }

  public void setQuestion(Question question) {
    this.question = question;
  }

  public List<DuelPlayerView> getPlayers() {
    return players;
  }

  public void setPlayers(List<DuelPlayerView> players) {
    this.players = players;
  }

  public List<DuelRoundResultView> getRoundResults() {
    return roundResults;
  }

  public void setRoundResults(List<DuelRoundResultView> roundResults) {
    this.roundResults = roundResults;
  }

  public DuelResultView getResult() {
    return result;
  }

  public void setResult(DuelResultView result) {
    this.result = result;
  }

  public static class DuelPlayerView {
    private Integer userId;
    private String username;
    private Integer coinBalance;
    private Integer committedBet;
    private Boolean answered;

    public Integer getUserId() {
      return userId;
    }

    public void setUserId(Integer userId) {
      this.userId = userId;
    }

    public String getUsername() {
      return username;
    }

    public void setUsername(String username) {
      this.username = username;
    }

    public Integer getCoinBalance() {
      return coinBalance;
    }

    public void setCoinBalance(Integer coinBalance) {
      this.coinBalance = coinBalance;
    }

    public Integer getCommittedBet() {
      return committedBet;
    }

    public void setCommittedBet(Integer committedBet) {
      this.committedBet = committedBet;
    }

    public Boolean getAnswered() {
      return answered;
    }

    public void setAnswered(Boolean answered) {
      this.answered = answered;
    }
  }

  public static class DuelResultView {
    private Boolean draw;
    private Integer winnerUserId;
    private String winnerUsername;
    private Integer loserUserId;
    private String loserUsername;
    private Integer payout;
    private String text;

    public Boolean getDraw() {
      return draw;
    }

    public void setDraw(Boolean draw) {
      this.draw = draw;
    }

    public Integer getWinnerUserId() {
      return winnerUserId;
    }

    public void setWinnerUserId(Integer winnerUserId) {
      this.winnerUserId = winnerUserId;
    }

    public String getWinnerUsername() {
      return winnerUsername;
    }

    public void setWinnerUsername(String winnerUsername) {
      this.winnerUsername = winnerUsername;
    }

    public Integer getLoserUserId() {
      return loserUserId;
    }

    public void setLoserUserId(Integer loserUserId) {
      this.loserUserId = loserUserId;
    }

    public String getLoserUsername() {
      return loserUsername;
    }

    public void setLoserUsername(String loserUsername) {
      this.loserUsername = loserUsername;
    }

    public Integer getPayout() {
      return payout;
    }

    public void setPayout(Integer payout) {
      this.payout = payout;
    }

    public String getText() {
      return text;
    }

    public void setText(String text) {
      this.text = text;
    }
  }

  public static class DuelRoundResultView {
    private Integer roundNumber;
    private Integer questionNumber;
    private String questionText;
    private String outcome;
    private String firstPlayerName;
    private Boolean firstPlayerCorrect;
    private String secondPlayerName;
    private Boolean secondPlayerCorrect;
    private Integer pot;
    private Integer payout;

    public Integer getRoundNumber() {
      return roundNumber;
    }

    public void setRoundNumber(Integer roundNumber) {
      this.roundNumber = roundNumber;
    }

    public Integer getQuestionNumber() {
      return questionNumber;
    }

    public void setQuestionNumber(Integer questionNumber) {
      this.questionNumber = questionNumber;
    }

    public String getQuestionText() {
      return questionText;
    }

    public void setQuestionText(String questionText) {
      this.questionText = questionText;
    }

    public String getOutcome() {
      return outcome;
    }

    public void setOutcome(String outcome) {
      this.outcome = outcome;
    }

    public String getFirstPlayerName() {
      return firstPlayerName;
    }

    public void setFirstPlayerName(String firstPlayerName) {
      this.firstPlayerName = firstPlayerName;
    }

    public Boolean getFirstPlayerCorrect() {
      return firstPlayerCorrect;
    }

    public void setFirstPlayerCorrect(Boolean firstPlayerCorrect) {
      this.firstPlayerCorrect = firstPlayerCorrect;
    }

    public String getSecondPlayerName() {
      return secondPlayerName;
    }

    public void setSecondPlayerName(String secondPlayerName) {
      this.secondPlayerName = secondPlayerName;
    }

    public Boolean getSecondPlayerCorrect() {
      return secondPlayerCorrect;
    }

    public void setSecondPlayerCorrect(Boolean secondPlayerCorrect) {
      this.secondPlayerCorrect = secondPlayerCorrect;
    }

    public Integer getPot() {
      return pot;
    }

    public void setPot(Integer pot) {
      this.pot = pot;
    }

    public Integer getPayout() {
      return payout;
    }

    public void setPayout(Integer payout) {
      this.payout = payout;
    }
  }
}


