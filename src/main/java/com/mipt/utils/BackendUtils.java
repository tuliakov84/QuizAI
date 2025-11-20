package com.mipt.utils;

import com.mipt.dbAPI.DatabaseAccessException;
import com.mipt.dbAPI.DbService;

import com.mipt.domainModel.Game;

import java.sql.SQLException;
import java.time.Instant;
import java.util.List;

public class BackendUtils {

  private static final int MAX_SESSION_RETRIES = 5;

  private final DbService dbService;

  public BackendUtils(DbService dbService) {
    this.dbService = dbService;
  }

  public void logOut(String session) throws AccessException {
    try {
      dbService.logOut(session);
    } catch (DatabaseAccessException e) {
      throw new AccessException("Failed to logout: " + e.getMessage());
    } catch (SQLException e) {
      throw new AccessException("Database error while logging out");
    }
  }

  public Game buildGame(String hostSession, int topicId, Game.LevelDifficulty level, boolean isPrivate, int maxAmountOfPlayers) {
    throw new UnsupportedOperationException("createGame is not implemented yet");
  }


  public void changeMaxAmountOfPlayersInGame(String hostSession, int newMaxAmountOfPlayers) {
    throw new UnsupportedOperationException("changeMaxAmountOfPlayersInGame is not implemented yet");
  }


  public void startGame(String hostSession) {
    throw new UnsupportedOperationException("startGame is not implemented yet");
  }


  public void deleteGame(String hostSession) {
    throw new UnsupportedOperationException("deleteGame is not implemented yet");
  }


  public void pauseGame(String hostSession) {
    throw new UnsupportedOperationException("pauseGame is not implemented yet");
  }


  public void resumeGame(String hostSession) {
    throw new UnsupportedOperationException("resumeGame is not implemented yet");
  }


  public void joinGame(String session, int gameId) throws AccessException {
    try {
      dbService.setCurrentGame(session, gameId);
    } catch (SQLException e) {
      throw new AccessException("Database error while joining the game with id " + gameId);
    } catch (DatabaseAccessException e) {
      throw new AccessException("Failed to join game with id " + gameId);
    }
  }

  public void leaveGame(String session) throws AccessException {
    try {
      dbService.setCurrentGame(session, null);
    } catch (SQLException e) {
      throw new AccessException("Database error while leaving the game");
    } catch (DatabaseAccessException e) {
      throw new AccessException("Failed to leave game");
    }
  }


  public List<Integer> getListOfPlayersInCurrentGame(int gameId) {
    throw new UnsupportedOperationException("getListOfPlayersInCurrentGame is not implemented yet");
  }


  public void getNextQuestion(int questionId, int gameId) {
    throw new UnsupportedOperationException("getNextQuestion is not implemented yet");
  }


  public void checkAnswer(String session, int answerNumber, Instant wastedTime) {
    throw new UnsupportedOperationException("getAnswer is not implemented yet");
  }


  public int getCurrentGamePoints(String session) {
    throw new UnsupportedOperationException("getCurrentGamePoints is not implemented yet");
  }


  public void endGame(int gameId) {
    throw new UnsupportedOperationException("endGame is not implemented yet");
  }

  public boolean isSessionCollision(DatabaseAccessException exception) {
    String message = exception.getMessage();
    return message != null && message.contains("Session key is not unique");
  }

  public String generateSessionId() {
    return java.util.UUID.randomUUID().toString().replace("-", "");
  }
}
