package com.mipt.api;

import com.mipt.dbAPI.DatabaseAccessException;
import com.mipt.dbAPI.DbService;
import com.mipt.domainModel.*;
import com.mipt.initialization.TopicsInit;
import com.mipt.utils.BackendUtils;
import com.mipt.utils.ValidationUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    TopicsInit topicsInit = new TopicsInit(dbService);
    topicsInit.topicsInit();
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
      String username = user.getUsername();
      String password = user.getPassword();
      if (!ValidationUtils.passwordValidation(password)) {
        return new ResponseEntity<>("Password validation error. Bad password", HttpStatus.BAD_REQUEST);
      }
      if (!ValidationUtils.usernameValidation(username)) {
        return new ResponseEntity<>("Username validation error. Bad username", HttpStatus.BAD_REQUEST);
      }
      dbService.register(username, password);
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
        return new ResponseEntity<>("Description validation error. Bad description", HttpStatus.BAD_REQUEST);
      }
      dbService.changeDescription(session, description);
      return new ResponseEntity<>(HttpStatus.OK);
    } catch (SQLException e) {
      return new ResponseEntity<>("Database error occurred while configuring account " + user.getUsername() + "': " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    } catch (DatabaseAccessException e) {
      return new ResponseEntity<>("Failed to configure information about user " + e.getMessage(), HttpStatus.NOT_FOUND);
    }
  }

  @PostMapping("/users/set/password")
  public ResponseEntity<Object> changePassword(@RequestBody User user) {
    try {
      String session = user.getSession();
      String password = user.getPassword();
      if (!ValidationUtils.passwordValidation(password)) {
        return new ResponseEntity<>("Password validation error. Bad password", HttpStatus.BAD_REQUEST);
      }
      dbService.changePassword(session, password);

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
      Integer[] gameIds = dbService.getGamesPlayed(user.getSession());
      Game[] games = new Game[gameIds.length];
      for (int i = 0; i < gameIds.length; i++) {
        Game inpGame = new Game();
        Integer gameId = gameIds[i];
        inpGame.setGameId(gameId);
        Integer[] preset = dbService.getPreset(gameId);
        inpGame.setLevelDifficulty(preset[1]);
        inpGame.setNumberOfQuestions(preset[2]);
        inpGame.setParticipantsNumber(preset[3]);
        inpGame.setTopicId(preset[4]);
        inpGame.setGameEndTime(dbService.getGameEndTime(gameId).toInstant());

        games[i] = inpGame;
      }
      return new ResponseEntity<>(games, HttpStatus.OK);
    } catch (SQLException e) {
      return new ResponseEntity<>("Database error occurred while getting information about " + user.getUsername() + "': " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    } catch (DatabaseAccessException e) {
      return new ResponseEntity<>("Failed to get information about user " + e.getMessage(), HttpStatus.NOT_FOUND);
    }
  }

  @PostMapping("/game/join")
  public ResponseEntity<Object> joinRoom(@RequestBody RoomJoinObject data) {
    try {
      String session = data.getSession();
      int gameId = data.getGameId();
      int status = dbService.getStatus(gameId);

      if (status == 0) {
        dbService.setCurrentGame(session, gameId);

        Integer[] preset = dbService.getPreset(gameId);
        data.setAuthorId(preset[0]);
        data.setLevelDifficulty(preset[1]);
        data.setNumberOfQuestions(preset[2]);
        data.setParticipantsNumber(preset[3]);
        data.setCurrentParticipantsNumber(dbService.getCurrentParticipantsNumber(gameId));
        int topicId = preset[4];
        data.setTopicId(topicId);
        data.setIsPrivate(dbService.getPrivate(gameId));

        return new ResponseEntity<>(data, HttpStatus.OK);
      } else {
        return new ResponseEntity<>("Failed to join room " + data.getGameId() + " because it's already started", HttpStatus.CONFLICT);
      }
    } catch (DatabaseAccessException e) {
      return new ResponseEntity<>("Failed to join room '" + data.getGameId() + "': " + e.getMessage(), HttpStatus.NOT_FOUND);
    } catch (SQLException e) {
      return new ResponseEntity<>("Database error occurred while joining room '" + data.getGameId() + "'", HttpStatus.NOT_FOUND);
    }
  }

  @PostMapping("/game/create")
  public ResponseEntity<Object> createGame(@RequestBody RoomJoinObject data) {
    try {
      String sessionOfAuthor = data.getSession();

      int levelDifficulty;
      switch (data.getLevelDifficulty()) {
        case EASY -> levelDifficulty = 1;
        case MEDIUM -> levelDifficulty = 2;
        case HARD -> levelDifficulty = 3;
        default -> levelDifficulty = 0;
      }

      int numberOfQuestions = data.getNumberOfQuestions();
      int participantsNumber = data.getParticipantsNumber();
      int topicId = data.getTopicId();
      boolean isPrivate = data.getIsPrivate();
      int gameId = dbService.createGame(sessionOfAuthor, levelDifficulty,
          numberOfQuestions, participantsNumber, topicId, isPrivate);
      data.setGameId(gameId);
      data.setTopicId(topicId);
      Integer[] preset = dbService.getPreset(gameId);
      data.setAuthorId(preset[0]);

      // AI loadQuestions() METHOD NEEDED TO BE HERE !!!

      return joinRoom(data);
    } catch (DatabaseAccessException e) {
      return new ResponseEntity<>("Failed to create room '" + data.getGameId() + "': " + e.getMessage(), HttpStatus.NOT_FOUND);
    } catch (SQLException e) {
      return new ResponseEntity<>("Database error occurred while creating game '" + data.getGameId() + "'", HttpStatus.NOT_FOUND);
    }
  }

  @PostMapping("/game/set/participants-number")
  public ResponseEntity<Object> changeParticipantsNumber(@RequestBody RoomJoinObject data) {
    try {
      String session = data.getSession();
      int userId = dbService.getUserId(session);
      int participantsNumber = data.getParticipantsNumber();
      int gameId = data.getGameId();

      Integer[] preset = dbService.getPreset(gameId);
      if (preset[0] != userId) {
        return new ResponseEntity<>("User " + userId + " is not the author of game '" + gameId + "'", HttpStatus.BAD_REQUEST);
      }

      dbService.changeParticipantsNumber(gameId, participantsNumber);
      return new ResponseEntity<>(data, HttpStatus.OK);
    } catch (DatabaseAccessException e) {
      return new ResponseEntity<>("Failed to update room '" + data.getGameId() + "': " + e.getMessage(), HttpStatus.NOT_FOUND);
    } catch (SQLException e) {
      return new ResponseEntity<>("Database error occurred while updating game '" + data.getGameId() + "'", HttpStatus.NOT_FOUND);
    }
  }

  @PostMapping("/game/set/private")
  public ResponseEntity<Object> setPrivate(@RequestBody Game game) {
    try {
      int gameId = game.getGameId();
      boolean isPrivate = game.getIsPrivate();
      dbService.setPrivate(gameId, isPrivate);
      return new ResponseEntity<>(HttpStatus.OK);
    } catch (DatabaseAccessException e) {
      return new ResponseEntity<>("Failed to modify privateness option for game " + game.getGameId(), HttpStatus.NOT_FOUND);
    } catch (SQLException e) {
      return new ResponseEntity<>("Database error occurred while modifying privateness option for game " + game.getGameId(), HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @PostMapping("/game/get/lobby")
  public ResponseEntity<Object> getLobby(@RequestBody LobbyObject lobby) {
    try {
      int gameId = lobby.getGameId();
      lobby.setStatus(dbService.getStatus(gameId));
      List<String> usernames = List.of(dbService.getParticipantUsernames(gameId));
      lobby.setPlayersUsernames(usernames);
      return new ResponseEntity<>(lobby, HttpStatus.OK);
    } catch (DatabaseAccessException e) {
      return new ResponseEntity<>("Failed to modify privateness option for game " + lobby.getGameId(), HttpStatus.NOT_FOUND);
    } catch (SQLException e) {
      return new ResponseEntity<>("Database error occurred while modifying privateness option for game " + lobby.getGameId(), HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @PostMapping("/game/start")
  public ResponseEntity<Object> startGame(@RequestBody Game game) {
    try {
      int gameId = game.getGameId();
      dbService.setStatus(gameId, 2);
      dbService.setGameStartTime(gameId, Instant.now());
      return new ResponseEntity<>(HttpStatus.OK);
    } catch (DatabaseAccessException e) {
      return new ResponseEntity<>("Failed to start the game " + game.getGameId(), HttpStatus.NOT_FOUND);
    } catch (SQLException e) {
      return new ResponseEntity<>("Database error occurred while stating game " + game.getGameId(), HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @PostMapping("/game/get/question")
  public ResponseEntity<Object> getQuestion(@RequestBody Question question) {
    try {
      int gameId = question.getGameId();
      int questionNumber = question.getQuestionNumber();
      Question res = dbService.getQuestion(gameId, questionNumber);
      return new ResponseEntity<>(res, HttpStatus.OK);
    } catch (DatabaseAccessException e) {
      return new ResponseEntity<>("Failed to get question " + question.getQuestionNumber() + " for game " + question.getQuestionId(), HttpStatus.NOT_FOUND);
    } catch (SQLException e) {
      return new ResponseEntity<>("Database error occurred while getting question " + question.getQuestionNumber() + " for game " + question.getGameId(), HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @PostMapping("/game/verify-answer")
  public ResponseEntity<Object> verifyAnswer(@RequestBody Question question) {
    try {
      int gameId = question.getGameId();
      int questionNumber = question.getQuestionNumber();
      int submittedAnswerNumber = question.getSubmittedAnswerNumber();
      int rightAnswer = dbService.getRightAnswer(gameId, questionNumber);
      return new ResponseEntity<>(rightAnswer == submittedAnswerNumber, HttpStatus.OK);
    } catch (DatabaseAccessException e) {
      return new ResponseEntity<>("Failed to get verify " + question.getQuestionNumber() + " for game " + question.getQuestionId(), HttpStatus.NOT_FOUND);
    } catch (SQLException e) {
      return new ResponseEntity<>("Database error occurred while verifying question " + question.getQuestionNumber() + " for game " + question.getGameId(), HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @PostMapping("/game/pause")
  public ResponseEntity<Object> pauseGame(@RequestBody Game game) {
    try {
      int gameId = game.getGameId();
      dbService.setStatus(gameId, 1);
      return new ResponseEntity<>(HttpStatus.OK);
    } catch (DatabaseAccessException e) {
      return new ResponseEntity<>("Failed to pause the game " + game.getGameId(), HttpStatus.NOT_FOUND);
    } catch (SQLException e) {
      return new ResponseEntity<>("Database error occurred while pausing game " + game.getGameId(), HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @PostMapping("/game/resume")
  public ResponseEntity<Object> resumeGame(@RequestBody Game game) {
    try {
      int gameId = game.getGameId();
      dbService.setStatus(gameId, 2);
      return new ResponseEntity<>(HttpStatus.OK);
    } catch (DatabaseAccessException e) {
      return new ResponseEntity<>("Failed to resume the game " + game.getGameId(), HttpStatus.NOT_FOUND);
    } catch (SQLException e) {
      return new ResponseEntity<>("Database error occurred while pausing game " + game.getGameId(), HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @PostMapping("/game/stop")
  public ResponseEntity<Object> stopGame(@RequestBody Game game) {
    try {
      int gameId = game.getGameId();
      dbService.stopGame(gameId);
      return new ResponseEntity<>(HttpStatus.OK);
    } catch (DatabaseAccessException e) {
      return new ResponseEntity<>("Failed to stop the game " + game.getGameId(), HttpStatus.NOT_FOUND);
    } catch (SQLException e) {
      return new ResponseEntity<>("Database error occurred while stopping game " + game.getGameId(), HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @PostMapping("/topic/get")
  public ResponseEntity<Object> getTopic(@RequestBody Topic topic) {
    try {
      int topicId = topic.getTopicId();
      Topic res = dbService.getTopicById(topicId);
      return new ResponseEntity<>(res, HttpStatus.OK);
    } catch (DatabaseAccessException e) {
      return new ResponseEntity<>("Failed to get the topic with id " + topic.getTopicId(), HttpStatus.NOT_FOUND);
    } catch (SQLException e) {
      return new ResponseEntity<>("Database error occurred while getting topic with id " + topic.getTopicId(), HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @PostMapping("/topic/get-all")
  public ResponseEntity<Object> getAllTopics() {
    try {
      List<Topic> res = List.of(dbService.getAllTopics());
      return new ResponseEntity<>(res, HttpStatus.OK);
    } catch (SQLException e) {
      return new ResponseEntity<>("Database error occurred while getting all topics", HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }
}
