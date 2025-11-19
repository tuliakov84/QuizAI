package com.mipt.api;

import com.mipt.dbAPI.DatabaseAccessException;
import com.mipt.dbAPI.DbService;
import com.mipt.domainModel.Achievement;
import com.mipt.domainModel.Game;
import com.mipt.domainModel.Question;
import com.mipt.domainModel.User;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.security.SecureRandom;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.List;

@RestController
public class ApiController implements IApiController {

  private static final int SESSION_BYTES = 24;
  private static final int MAX_SESSION_RETRIES = 5;

  private final DbService dbService;
  private final SecureRandom secureRandom = new SecureRandom();

  public ApiController(DbService dbService) {
    this.dbService = dbService;
  }

  @PostMapping("/api/auth/login")
  public ResponseEntity<Map<String, String>> loginEndpoint(@RequestBody AuthRequest request) {
    if (request == null || request.getUsername() == null || request.getPassword() == null) {
      return ResponseEntity.badRequest().body(Map.of("error", "Username and password are required"));
    }
    try {
      String session = login(request.getUsername(), request.getPassword());
      return ResponseEntity.ok(Map.of("session", session));
    } catch (RuntimeException e) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(Map.of("error", e.getMessage()));
    }
  }

  @PostMapping("/api/auth/register")
  public ResponseEntity<Map<String, String>> registerEndpoint(@RequestBody AuthRequest request) {
    if (request == null || request.getUsername() == null || request.getPassword() == null) {
      return ResponseEntity.badRequest().body(Map.of("error", "Username and password are required"));
    }
    try {
      String session = register(request.getUsername(), request.getPassword());
      return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("session", session));
    } catch (RuntimeException e) {
      HttpStatus status = e.getMessage() != null && e.getMessage().contains("already")
          ? HttpStatus.CONFLICT
          : HttpStatus.INTERNAL_SERVER_ERROR;
      return ResponseEntity.status(status).body(Map.of("error", e.getMessage()));
    }
  }

  @Override
  public String login(String userName, String password) {
    for (int attempt = 0; attempt < MAX_SESSION_RETRIES; attempt++) {
      String session = generateSessionId();
      try {
        dbService.authorize(userName, password, session);
        return session;
      } catch (DatabaseAccessException e) {
        if (isSessionCollision(e)) {
          continue;
        }
        throw new RuntimeException("Failed to authorize user '" + userName + "': " + e.getMessage(), e);
      } catch (SQLException e) {
        throw new RuntimeException("Database error while authorizing user '" + userName + "'", e);
      }
    }
    throw new RuntimeException("Unable to generate unique session token");
  }

  @Override
  public String register(String userName, String password) {
    try {
      dbService.register(userName, password);
      return login(userName, password);
    } catch (DatabaseAccessException e) {
      throw new RuntimeException("Failed to register user '" + userName + "': " + e.getMessage(), e);
    } catch (SQLException e) {
      throw new RuntimeException("Database error while registering user '" + userName + "'", e);
    }
  }

  @Override
  public void logout(String session) {
    throw new UnsupportedOperationException("logout is not implemented yet");
  }

  @Override
  public String getUsername(String session) {
    throw new UnsupportedOperationException("getUsername is not implemented yet");
  }

  @Override
  public int getProfilePicId(String session) {
    throw new UnsupportedOperationException("getProfilePicId is not implemented yet");
  }

  @Override
  public int getCurrentGlobalPoints(String session) {
    throw new UnsupportedOperationException("getCurrentGlobalPoints is not implemented yet");
  }

  @Override
  public int getGlobalPossiblePoints(String session) {
    throw new UnsupportedOperationException("getGlobalPossiblePoints is not implemented yet");
  }

  @Override
  public String getDescription(String session) {
    throw new UnsupportedOperationException("getDescription is not implemented yet");
  }

  @Override
  public List<Achievement> getAchievements(String session) {
    throw new UnsupportedOperationException("getAchievements is not implemented yet");
  }

  @Override
  public List<Game> getGamesPlayed(String session) {
    throw new UnsupportedOperationException("getGamesPlayed is not implemented yet");
  }

  @Override
  public User changeUserProfilePic(String session, int picId) {
    throw new UnsupportedOperationException("changeUserProfilePic is not implemented yet");
  }

  @Override
  public User changeUsername(String session, String newUserName) {
    throw new UnsupportedOperationException("changeUsername is not implemented yet");
  }

  @Override
  public User changePassword(String session, String oldPassword, String newPassword) {
    throw new UnsupportedOperationException("changePassword is not implemented yet");
  }

  @Override
  public User changeDescription(String session, String newDescription) {
    throw new UnsupportedOperationException("changeDescription is not implemented yet");
  }

  @Override
  public Game createGame(String hostSession, int topicId, Game.LevelDifficulty level, boolean isPrivate, int maxAmountOfPlayers) {
    throw new UnsupportedOperationException("createGame is not implemented yet");
  }

  @Override
  public Game changeMaxAmountOfPlayersInGame(String hostSession, int newMaxAmountOfPlayers) {
    throw new UnsupportedOperationException("changeMaxAmountOfPlayersInGame is not implemented yet");
  }

  @Override
  public Question startGame(String hostSession) {
    throw new UnsupportedOperationException("startGame is not implemented yet");
  }

  @Override
  public void deleteGame(String hostSession) {
    throw new UnsupportedOperationException("deleteGame is not implemented yet");
  }

  @Override
  public void pauseGame(String hostSession) {
    throw new UnsupportedOperationException("pauseGame is not implemented yet");
  }

  @Override
  public void unpauseGame(String hostSession) {
    throw new UnsupportedOperationException("unpauseGame is not implemented yet");
  }

  @Override
  public List<Game> getListOfOpenGames() {
    throw new UnsupportedOperationException("getListOfOpenGames is not implemented yet");
  }

  @Override
  public User joinGame(String session, int gameId) {
    throw new UnsupportedOperationException("joinGame is not implemented yet");
  }

  @Override
  public User leftGame(String session) {
    throw new UnsupportedOperationException("leftGame is not implemented yet");
  }

  @Override
  public List<User> getListOfPlayersInCurrentGame(int gameId) {
    throw new UnsupportedOperationException("getListOfPlayersInCurrentGame is not implemented yet");
  }

  @Override
  public Question getNextQuestion(int questionId, int gameId) {
    throw new UnsupportedOperationException("getNextQuestion is not implemented yet");
  }

  @Override
  public void getAnswer(String session, int answerNumber, Instant wastedTime) {
    throw new UnsupportedOperationException("getAnswer is not implemented yet");
  }

  @Override
  public int getCurrentGamePoints(String session) {
    throw new UnsupportedOperationException("getCurrentGamePoints is not implemented yet");
  }

  @Override
  public void endGame(int gameId) {
    throw new UnsupportedOperationException("endGame is not implemented yet");
  }

  @Override
  public List<User> getGlobalLeaderboard() {
    throw new UnsupportedOperationException("getGlobalLeaderboard is not implemented yet");
  }

  private boolean isSessionCollision(DatabaseAccessException exception) {
    String message = exception.getMessage();
    return message != null && message.contains("Session key is not unique");
  }

  private String generateSessionId() {
    byte[] buffer = new byte[SESSION_BYTES];
    secureRandom.nextBytes(buffer);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(buffer);
  }

  public static class AuthRequest {
    private String username;
    private String password;

    public String getUsername() {
      return username;
    }

    public void setUsername(String username) {
      this.username = username;
    }

    public String getPassword() {
      return password;
    }

    public void setPassword(String password) {
      this.password = password;
    }
  }
}

