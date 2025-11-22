package com.mipt.api;

import com.mipt.dbAPI.DatabaseAccessException;
import com.mipt.dbAPI.DbService;
import com.mipt.domainModel.*;
import com.mipt.utils.BackendUtils;
import com.mipt.utils.ValidationUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.swing.*;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;


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
        String password = user.getPassword();
        String username = user.getUsername();
        if (!ValidationUtils.passwordValidation(password)){
          return new ResponseEntity<>("Password validation error. Bad password", HttpStatus.BAD_REQUEST);
        }
        if (!ValidationUtils.usernameValidation(username)){
          return new ResponseEntity<>("Username validation error. Bad username", HttpStatus.BAD_REQUEST);
        }
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
        return new ResponseEntity<>(session, HttpStatus.OK);
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
      return new ResponseEntity<>("Database error occurred while registering user '" + user.getUsername() + "'", HttpStatus.INTERNAL_SERVER_ERROR);
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
      return new ResponseEntity<>("Database error occurred while logging out of account '" + user.getUsername() + "'", HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @PostMapping("/users/profile")
  public ResponseEntity<Object> getProfile(@RequestBody User user) {
    try {
      String session = user.getSession();
      user.setUserId(dbService.getUserId(session));

      Integer currentGameId = dbService.getCurrentGame(session);
      if (currentGameId != null) {
        Game currentGame = new Game();
        currentGame.setGameId(currentGameId);
        user.setCurrentGame(currentGame);
      }
      user.setAchievements(dbService.getAchievementsOf(session));
      Integer[] gamesPlayed = dbService.getGamesPlayed(session);
      user.setGamesPlayed(gamesPlayed);
      user.setPicId(dbService.getProfilePic(session));
      user.setDescription(dbService.getDescription(session));
      user.setUsername(dbService.getUsername(session));
      Timestamp lastActivity = dbService.getLastActivity(session);
      if (lastActivity != null) {
        user.setLastActivity(lastActivity.toInstant());
      }
      user.setGlobalPoints(dbService.getGlobalPoints(session));
      user.setGamesPlayedNumber(gamesPlayed.length);
      return new ResponseEntity<>(user, HttpStatus.OK);
    } catch (DatabaseAccessException e) {
      return new ResponseEntity<>("Failed to get information about account '" + user.getUsername() + "': " + e.getMessage(), HttpStatus.NOT_FOUND);
    } catch (SQLException e) {
      return new ResponseEntity<>("Database error occurred while getting information about account '" + user.getUsername() + "'", HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @PostMapping("/users/set/profile_pic")
  public ResponseEntity<Object> changeProfilePic(@RequestBody User user) {
    try {
      String session = user.getSession();
      Integer picId = user.getPicId();
      if (!picId.equals(0)) {
        dbService.changeProfilePic(session, picId);
        return new ResponseEntity<>(HttpStatus.OK);
      } else {
        return new ResponseEntity<>("Cant change to null picture", HttpStatus.NOT_FOUND);
      }
    } catch (SQLException e) {
      return new ResponseEntity<>("Database error occurred while configuring account " + user.getUsername() + "': " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    } catch (DatabaseAccessException e) {
      return new ResponseEntity<>("Failed to configure information about user " + e.getMessage(), HttpStatus.NOT_FOUND);
    }
  }

  @PostMapping("/users/set/description")
  public ResponseEntity<Object> changeDescription(@RequestBody User user) {
    try {
      String session = user.getSession();
      String description = user.getDescription();
      if (!ValidationUtils.descriptionValidation(description)) {
        return new ResponseEntity<>("説明検証エラー。説明エラー。説明が無効です。 perepishi", HttpStatus.BAD_REQUEST);
      }
      dbService.changeDescription(session, description);
      return new ResponseEntity<>(HttpStatus.OK);
    } catch (SQLException e) {
      return new ResponseEntity<>("Database error occurred while configuring account " + user.getUsername() + "': " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    } catch (DatabaseAccessException e) {
      return new ResponseEntity<>("Failed to configure information about user " + e.getMessage(), HttpStatus.NOT_FOUND);
    }
  }

  @PostMapping("/users/get-games")
  public ResponseEntity<Object> getGames(@RequestBody User user) {
    try {
      Integer[] games = dbService.getGamesPlayed(user.getSession());
      return new ResponseEntity<>(games, HttpStatus.OK);
    } catch (SQLException e) {
      return new ResponseEntity<>("Database error occurred while getting information about " + user.getUsername() + "': " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    } catch (DatabaseAccessException e) {
      return new ResponseEntity<>("Failed to get information about user " + e.getMessage(), HttpStatus.NOT_FOUND);
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

        game.setNumberOfQuestions(preset[2]);
        game.setParticipantsNumber(preset[3]);
        game.setCurrentParticipantsNumber(dbService.getCurrentParticipantsNumber(gameId));
        // check if topicId is null
        int topicId = preset[4];
        game.setTopicId(topicId);
        game.setPrivate(dbService.getPrivate(gameId));

        return new ResponseEntity<>(game, HttpStatus.OK);
      } else {
        return new ResponseEntity<>("Failed to join room " + game.getGameId() + " because it's already started", HttpStatus.CONFLICT);
      }
    } catch (DatabaseAccessException e) {
      return new ResponseEntity<>("Failed to join room '" + game.getGameId() + "': " + e.getMessage(), HttpStatus.NOT_FOUND);
    } catch (SQLException e) {
      return new ResponseEntity<>("Database error occurred while joining room '" + game.getGameId() + "'", HttpStatus.NOT_FOUND);
    }
  }
  @PostMapping("/game/create")
  public ResponseEntity<Object> createGame(@RequestBody User user, @RequestBody Game game, @RequestBody Topic topic) {
    try {
      String sessionOfAuthor = user.getSession();

      int levelDifficulty;
      switch (game.getLevelDifficulty()) {
        case EASY -> levelDifficulty = 1;
        case MEDIUM -> levelDifficulty = 2;
        case HARD -> levelDifficulty = 3;
        default -> levelDifficulty = 0;
      }

      int numberOfQuestions =  game.getNumberOfQuestions();
      int participantsNumber = game.getParticipantsNumber();
      int topicId = topic.getTopicId();
      int gameId = dbService.createGame(sessionOfAuthor, levelDifficulty,
          numberOfQuestions, participantsNumber, topicId);
      game.setGameId(gameId);
      game.setTopicId(topicId);
      
      return new ResponseEntity<>(game, HttpStatus.OK);
      } catch (DatabaseAccessException e) {
      return new ResponseEntity<>("Failed to create room '" + game.getGameId() + "': " + e.getMessage(), HttpStatus.NOT_FOUND);
    } catch (SQLException e) {
      return new ResponseEntity<>("Database error occurred while creating game '" + game.getGameId() + "'", HttpStatus.NOT_FOUND);
    }
  }
}
