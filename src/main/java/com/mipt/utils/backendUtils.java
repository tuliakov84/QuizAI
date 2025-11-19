package com.mipt.utils;

import com.mipt.dbAPI.DatabaseAccessException;
import com.mipt.dbAPI.DbService;

import com.mipt.domainModel.Game;
import org.springframework.web.bind.annotation.RestController;

import java.sql.SQLException;
import java.time.Instant;
import java.util.List;

@RestController
public class backendUtils {

  private static final int MAX_SESSION_RETRIES = 5;

  private final DbService dbService;

  public backendUtils(DbService dbService) {
    this.dbService = dbService;
  }


  public String login(String userName, String password) throws AccessException {
    for (int attempt = 0; attempt < MAX_SESSION_RETRIES; attempt++) {
      String session = generateSessionId();
      try {
        dbService.authenticate(userName, password, session);
        return session;
      } catch (DatabaseAccessException e) {
        if (isSessionCollision(e)) {
          continue;
        }
        throw new AccessException("Failed to authorize user '" + userName + "': " + e.getMessage());
      } catch (SQLException e) {
        throw new AccessException("Database error while authorizing user '" + userName + "'");
      }
    }
    throw new AccessException("Unable to generate unique session token");
  }


  public String register(String userName, String password) throws AccessException {
    try {
      dbService.register(userName, password);
      return login(userName, password);
    } catch (DatabaseAccessException e) {
      throw new AccessException("Failed to register user '" + userName + "': " + e.getMessage());
    } catch (SQLException e) {
      throw new AccessException("Database error while registering user '" + userName + "'");
    }
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


  public void joinGame(String session, int gameId) {
    throw new UnsupportedOperationException("joinGame is not implemented yet");
  }


  public void leftGame(String session) {
    throw new UnsupportedOperationException("leftGame is not implemented yet");
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

  private boolean isSessionCollision(DatabaseAccessException exception) {
    String message = exception.getMessage();
    return message != null && message.contains("Session key is not unique");
  }

  private String generateSessionId() {
    return java.util.UUID.randomUUID().toString().replace("-", "");
  }
}
