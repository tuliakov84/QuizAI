package com.mipt.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mipt.dbAPI.DatabaseAccessException;
import com.mipt.dbAPI.DbService;
import com.mipt.domainModel.*;
import com.mipt.initialization.AchievementsInit;
import com.mipt.initialization.TopicsInit;
import com.mipt.utils.BackendUtils;
import com.mipt.utils.ValidationUtils;
import org.json.JSONArray;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;


@RestController
@RequestMapping("/api")
public class ApiController {
  private static final int MAX_SESSION_RETRIES = 5;

  private final BackendUtils utils;
  private final DbService dbService;

  /**
   * Wires the controller with the database layer and ensures that the topics
   * table is populated before serving requests.
   */
  public ApiController(DbService dbService) {
    this.dbService = dbService;
    this.utils = new BackendUtils();

    TopicsInit topicsInit = new TopicsInit(dbService);
    AchievementsInit achievementsInit = new AchievementsInit(dbService);
    topicsInit.topicsInit();
    achievementsInit.achievementsInit();
  }

  /**
   * Authenticates a user via username/password and establishes a new session
   * token. Retries on session collisions and returns the hydrated {@link User}.
   */
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

  /**
   * Registers a new account after validating username and password, then logs
   * the user in to provide the usual session payload.
   */
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

  /**
   * Revokes the active session for the supplied user.
   */
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

  /**
   * Returns an enriched snapshot of the requesting user profile, including
   * achievements, stats, avatar and currently joined game.
   */
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

  /**
   * Updates the profile picture reference for the active session.
   */
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

  /**
   * Persists a validated free-form profile description for the user.
   */
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

  /**
   * Changes the user password after running standard password validation.
   */
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

  /**
   * Returns historical game metadata for the requesting user, derived from
   * per-game presets and cached end times.
   */
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

  /**
   * Joins an open lobby if it has not started yet and returns the resolved game
   * preset together with lobby occupancy and privacy info.
   */
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

  /**
   * Removes the current user from their game lobby.
   */
  @PostMapping("/user/leave")
  public ResponseEntity<Object> leaveRoom(@RequestBody User user) {
    try {
      String session = user.getSession();
      dbService.leaveGame(session);
      return new ResponseEntity<>(HttpStatus.OK);
    } catch (DatabaseAccessException e) {
      return new ResponseEntity<>("Failed to leave the game", HttpStatus.NOT_FOUND);
    } catch (SQLException e) {
      return new ResponseEntity<>("Database error occurred while leaving the game", HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * Lists open games for the selected topic, allowing room discovery.
   */
  @PostMapping("/game/get-open")
  public ResponseEntity<Object> getOpenGames(@RequestBody Topic topic) {
    try {
      int topicId = topic.getTopicId();
      return new ResponseEntity<>(dbService.getOpenGames(topicId).toString(), HttpStatus.OK);
    } catch (DatabaseAccessException e) {
      return new ResponseEntity<>("Failed to get open games", HttpStatus.NOT_FOUND);
    } catch (SQLException e) {
      return new ResponseEntity<>("Database error occurred while getting open games", HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * Creates a new lobby owned by the author session and reuses join logic to
   * return the hydrated room payload.
   */
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

  /**
   * Allows a game author to modify the lobby capacity.
   */
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

  /**
   * Toggles the privacy flag for a game.
   */
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

  /**
   * Retrieves lobby metadata including current status and participant usernames.
   */
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

  /**
   * Moves a game into the running state and records the start timestamp.
   */
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

  /**
   * Fetches a single question by its sequential number inside a game.
   */
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

  /**
   * Checks whether the submitted answer index matches the stored correct answer + counts points user will get.
   */
  @PostMapping("/game/verify-answer")
  public ResponseEntity<Object> verifyAnswer(@RequestBody AnswerObject answerObject) {
    //todo убрать передачу сложности
    try {
      int gameId = answerObject.getGameId();
      int questionNumber = answerObject.getQuestionNumber();
      int submittedAnswerNumber = answerObject.getSubmittedAnswerNumber();
      int rightAnswer = dbService.getRightAnswer(gameId, questionNumber);

      Game.LevelDifficulty levelDifficulty = answerObject.getLevelDifficulty();
      String session = answerObject.getSession();

      if (submittedAnswerNumber == rightAnswer) {
        int timeTakenToAnswerInSeconds = answerObject.getTimeTakenToAnswerInSeconds();
        int pointsForAnswer = utils.countPoints(levelDifficulty, timeTakenToAnswerInSeconds);

        dbService.addCurrentGamePoints(session, pointsForAnswer);
        dbService.addGlobalPoints(session, pointsForAnswer);
      }

      int possiblePointsForAnswer = utils.countPossiblePoints(levelDifficulty);
      dbService.addGlobalPossiblePoints(session, possiblePointsForAnswer);
      return new ResponseEntity<>(HttpStatus.OK);
    } catch (DatabaseAccessException e) {
      return new ResponseEntity<>("Failed to verify " + answerObject.getQuestionNumber() + " for game " + answerObject.getQuestionId(), HttpStatus.NOT_FOUND);
    } catch (SQLException e) {
      return new ResponseEntity<>("Database error occurred while verifying question " + answerObject.getQuestionNumber() + " for game " + answerObject.getGameId(), HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * Puts a game into the paused status.
   */
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

  /**
   * Resumes a paused game.
   */
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

  /**
   * Stops an active game without deleting it.
   */
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

  /**
   * Stops and then permanently deletes the specified game.
   */
  @PostMapping("/game/delete")
  public ResponseEntity<Object> deleteGame(@RequestBody Game game) {
    try {
      int gameId = game.getGameId();
      dbService.stopGame(gameId);
      dbService.deleteGame(gameId);
      return new ResponseEntity<>(HttpStatus.OK);
    } catch (DatabaseAccessException e) {
      return new ResponseEntity<>("Failed to stop the game " + game.getGameId(), HttpStatus.NOT_FOUND);
    } catch (SQLException e) {
      return new ResponseEntity<>("Database error occurred while stopping game " + game.getGameId(), HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * Returns a single topic description by id.
   */
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

  /**
   * Returns the catalog of all available topics.
   */
  @PostMapping("/topic/get-all")
  public ResponseEntity<Object> getAllTopics() {
    try {
      List<Topic> res = List.of(dbService.getAllTopics());
      return new ResponseEntity<>(res, HttpStatus.OK);
    } catch (SQLException e) {
      return new ResponseEntity<>("Database error occurred while getting all topics", HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * Returns the per-game leaderboard as a JSON string for compatibility with
   * the web client.
   */
  @PostMapping("/leaderboard/get/by-game")
  public ResponseEntity<Object> getLeaderboardsByGame(@RequestBody Game game) {
    try {
      int gameId = game.getGameId();
      JSONArray res = dbService.getGameLeaderboards(gameId);
      return new ResponseEntity<>(res.toString(), HttpStatus.OK);
    } catch (SQLException e) {
      return new ResponseEntity<>("Database error occurred while getting the leaderboards of game " + game.getGameId(), HttpStatus.INTERNAL_SERVER_ERROR);
    } catch (DatabaseAccessException e) {
      return new ResponseEntity<>("Failed to get the leaderboards of game " + game.getGameId(), HttpStatus.NOT_FOUND);
    }
  }

  /**
   * Serves the static global leaderboard file bundled with the application.
   */
  @PostMapping("/leaderboard/get/global")
  public ResponseEntity<Object> getGlobalLeaderboards() {
    try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("global-leaderboards.json")) {
      if (inputStream == null) {
        throw new FileNotFoundException("File global-leaderboards.json not found in classpath");
      }

      String jsonContent = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
      new ObjectMapper().readValue(jsonContent, Object.class);

      return new ResponseEntity<>(jsonContent, HttpStatus.OK);
    } catch (Exception e) {
      System.out.println("Error reading global-leaderboards.json: " + e.getMessage());
      return new ResponseEntity<>("Error reading global leaderboards", HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }
  
  /**
   * Lists all achievements supported by the platform.
   */
  @PostMapping("/achievement/get-all")
  public ResponseEntity<Object> getAllAchievements() {
    try {
      List<Achievement> res = List.of(dbService.getAllAchievements());
      return new ResponseEntity<>(res, HttpStatus.OK);
    } catch (SQLException e) {
      return new ResponseEntity<>("Database error occurred while getting all achievements", HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * Resolves a list of achievement ids into their full DTO representations.
   */
  @PostMapping("/achievement/get")
  public ResponseEntity<Object> getAchievementList(@RequestBody List<Integer> achievementIds) {
    try {
      ArrayList<Achievement> res = new ArrayList<>();
      for (Integer achievementId : achievementIds) {
        res.add(dbService.getAchievementById(achievementId));
      }
      return new ResponseEntity<>(res, HttpStatus.OK);
    } catch (DatabaseAccessException e) {
      return new ResponseEntity<>("Failed to get the achievements", HttpStatus.NOT_FOUND);
    } catch (SQLException e) {
      return new ResponseEntity<>("Database error occurred while getting achievements", HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * Determines which achievement conditions were just satisfied and returns the
   * corresponding achievement objects.
   */
  @PostMapping("/achievement/check-achieved")
  public ResponseEntity<Object> checkAchieved(@RequestBody Achieved achieved) {
    try {
      Integer[] achievementIds = dbService.checkAchievementAchieved(achieved.getSession(), achieved);
      return getAchievementList(List.of(achievementIds));
    } catch (SQLException e) {
      return new ResponseEntity<>("Database error occurred while getting all achievements", HttpStatus.INTERNAL_SERVER_ERROR);
    } catch (DatabaseAccessException e) {
    return new ResponseEntity<>("Failed to get the achievements", HttpStatus.NOT_FOUND);
    }
  }

  /**
   * Bulk-attaches a list of achievements to the provided user sessions.
   */
  @PostMapping("/achievement/attach")
  public ResponseEntity<Object> attachAchievementList(@RequestBody List<Achieved> achievements) {
    try {
      for (Achieved achievement : achievements) {
        dbService.attachAchievement(achievement.getSession(), achievement.getAchievementId());
      }
      return new ResponseEntity<>(HttpStatus.OK);
    } catch (DatabaseAccessException e) {
      return new ResponseEntity<>("Failed to attach the achievements", HttpStatus.NOT_FOUND);
    } catch (SQLException e) {
      return new ResponseEntity<>("Database error occurred while attaching achievements", HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @PostMapping("/unimaginably/painful/death")
  public ResponseEntity<Object> eraseAllData() {
    try {
      dbService.eraseAllData("DELETE_ALL_RECORDS_IN_DATABASE");
      return new ResponseEntity<>(HttpStatus.OK);
    } catch (SQLException e) {
      return new ResponseEntity<>("Database error occurred while deleting all data", HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }
}
