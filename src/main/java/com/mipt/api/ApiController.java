package com.mipt.api;

import com.mipt.dbAPI.DatabaseAccessException;
import com.mipt.dbAPI.DbService;
import com.mipt.domainModel.Game;
import com.mipt.domainModel.User;
import com.mipt.utils.AccessException;
import com.mipt.utils.BackendUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.SQLException;


@RestController
@RequestMapping("/api")
public class ApiController {
  private static final int MAX_SESSION_RETRIES = 5;

  private final BackendUtils utils;
  private final DbService dbService;

  public ApiController(DbService dbService) {
    this.dbService = dbService;
    this.utils = new BackendUtils(dbService);
  }

  @PostMapping("/auth/login")
  public ResponseEntity<Object> auth(@RequestBody User user) {
    for (int attempt = 0; attempt < MAX_SESSION_RETRIES; attempt++) {
      String session = utils.generateSessionId();
      try {
        dbService.authenticate(user.getUsername(), user.getPassword(), session);
        user.setSession(session);
        user.setUserId(dbService.getUserId(session));
        Integer currentGameId = dbService.getCurrentGame(session);
        if (currentGameId != null) {
          Game currentGame = new Game();
          currentGame.setGameId(currentGameId);
          user.setCurrentGame(currentGame);
        }
        user.setDescription(dbService.getDescription(session));
        user.setGlobalPoints(dbService.getGlobalPoints(session));
        return new ResponseEntity<>(user, HttpStatus.OK);
      } catch (DatabaseAccessException e) {
        if (utils.isSessionCollision(e)) {
          continue;
        }
        return new ResponseEntity<>("Failed to authenticate user '" + user.getUsername() + "': " + e.getMessage(), HttpStatus.NOT_FOUND);
      } catch (SQLException e) {
        return new ResponseEntity<>("Database error occurred while authenticating user '" + user.getUsername() + "'", HttpStatus.INTERNAL_SERVER_ERROR);
      }
    }
    return new ResponseEntity<>("Unable to generate unique session token", HttpStatus.INTERNAL_SERVER_ERROR);
  }

  @PostMapping("/auth/register")
  public ResponseEntity<Object> register(@RequestBody User user) {
    try {
      dbService.register(user.getUsername(), user.getPassword());
      return auth(user);
    } catch (DatabaseAccessException e) {
      return new ResponseEntity<>("Failed to register user '" + user.getUsername() + "': " + e.getMessage(), HttpStatus.NOT_FOUND);
    } catch (SQLException e) {
      return new ResponseEntity<>("Database error occurred while registering user '" + user.getUsername() + "'", HttpStatus.NOT_FOUND);
    }
  }

  @PostMapping("/auth/logout")
  public ResponseEntity<Object> logout(@RequestBody User user) {
    try {
      dbService.logOut(user.getSession());
      return new ResponseEntity<>(HttpStatus.OK);
    } catch (DatabaseAccessException e) {
      return new ResponseEntity<>("Failed to log out of account '" + user.getUsername() + "': " + e.getMessage(), HttpStatus.NOT_FOUND);
    } catch (SQLException e) {
      return new ResponseEntity<>("Database error occurred while logging out of account '" + user.getUsername() + "'", HttpStatus.NOT_FOUND);
    }
  }

  @PostMapping("/users/profile")
  public ResponseEntity<Object> profile(@RequestBody User user) {
    try {
      String session = user.getSession();
      user.setUserId(dbService.getUserId(session));

      Integer currentGameId = dbService.getCurrentGame(session);
      if (currentGameId != null) {
        Game currentGame = new Game();
        currentGame.setGameId(currentGameId);
        user.setCurrentGame(currentGame);
      }

      user.setDescription(dbService.getDescription(session));
      user.setGlobalPoints(dbService.getGlobalPoints(session));

      return new ResponseEntity<>(user, HttpStatus.OK);
    } catch (DatabaseAccessException e) {
      return new ResponseEntity<>("Failed to get information about account '" + user.getUsername() + "': " + e.getMessage(), HttpStatus.NOT_FOUND);
    } catch (SQLException e) {
      return new ResponseEntity<>("Database error occurred while getting information about account '" + user.getUsername() + "'", HttpStatus.NOT_FOUND);
    }
  }

  @PostMapping("/rooms/join")
  public ResponseEntity<Object> joinRoom(@RequestBody User user, @RequestBody Game game) {
    try {
      String session = user.getSession();
      int gameId = game.getGameId();
      int status = dbService.getStatus(gameId);

      if (status == 0) {

        dbService.setCurrentGame(session, gameId);

        Integer[] preset = dbService.getPreset(gameId);
        game.setAuthorId(preset[0]);

        switch (preset[1]) {
          case 1 -> game.setLevelDifficulty(Game.LevelDifficulty.EASY);
          case 2 -> game.setLevelDifficulty(Game.LevelDifficulty.MEDIUM);
          case 3 -> game.setLevelDifficulty(Game.LevelDifficulty.HARD);
        }

        //Add currentParticipants number to Game, DbService and ApiController

        game.setNumberOfQuestions(preset[2]);
        game.setParticipantsNumber(preset[3]);
        game.setCurrentParticipantsNumber(dbService.getCurrentParticipantsNumber(gameId));

        int topicId = preset[4];
        game.setTopic(dbService.getTopicById(topicId));

        game.setPrivate(dbService.getPrivate(gameId));

        return new ResponseEntity<>(game, HttpStatus.OK);
      } else {
        throw new AccessException();
      }
    } catch (DatabaseAccessException e) {
      return new ResponseEntity<>("Failed to join room '" + game.getGameId() + "': " + e.getMessage(), HttpStatus.NOT_FOUND);
    } catch (SQLException e) {
      return new ResponseEntity<>("Database error occurred while joining room '" + game.getGameId() + "'", HttpStatus.NOT_FOUND);
    } catch (AccessException e) {
      return new ResponseEntity<>("Game already started", HttpStatus.CONFLICT);
    }
  }
}
