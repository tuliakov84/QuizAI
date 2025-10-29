package com.mipt.dbAPI;

import java.sql.*;
import at.favre.lib.crypto.bcrypt.BCrypt;
import com.mipt.domainModel.*;
import org.json.JSONObject;
import org.json.JSONArray;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


public class DbService {
  // API of QUIZ AI ARENA database

  //
  // Initialization
  //

  private final Connection conn;

  public DbService(String url, String user, String password) {
    this.conn = connect(url, user, password);
  }

  private static Connection connect(String url, String user, String password) {
    // establishes connection to the database
    try {
      return DriverManager.getConnection(url, user, password);
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  //
  // Users TABLE REFERRED METHODS
  //

  public Integer getUserId(String session) throws SQLException {
    // checks if user authorized or not, returns userId if is
    // must be completed every query to check user authenticated to perform actions
    PreparedStatement selId = conn.prepareStatement("SELECT id FROM users WHERE session = ?");
    selId.setString(1, session);

    ResultSet rsSelId = selId.executeQuery();
    if (rsSelId.next()) {
      return !rsSelId.isBeforeFirst() ? rsSelId.getInt(1) : null; // returns userId if logged in, null if not logged
    } else {
      return null; // null if session not exists
    }
  }

  public boolean checkUserExists(String username) throws SQLException {
    PreparedStatement selExists = conn.prepareStatement("SELECT * FROM users WHERE username = ?");
    selExists.setString(1, username);
    ResultSet rsSelExists = selExists.executeQuery();
    return rsSelExists.isBeforeFirst(); // true if user already exists, false if not exists
  }

  public boolean register(String username, String password) throws SQLException {
    // creates an account for a new user
    if (checkUserExists(username)) {
      return false; // false if user already exists
    }
    String hashedPassword = BCrypt.withDefaults().hashToString(12, password.toCharArray());
    PreparedStatement inpData = conn.prepareStatement("INSERT INTO users (username, password, pic_id, description, games_played_number, global_points, global_possible_points, current_game_points) VALUES (?, ?, 0, '', 0, 0, 0, 0)");
    inpData.setString(1, username);
    inpData.setString(2, hashedPassword);
    inpData.executeUpdate();
    return true; // done
  }

  public boolean authorize(String username, String password, String session) throws SQLException {
    // authorizes user
    PreparedStatement selExists = conn.prepareStatement("SELECT password FROM users WHERE username = ?");
    selExists.setString(1, username);
    ResultSet rsSelExists = selExists.executeQuery();
    if (rsSelExists.next()) {
      String dbPassword = rsSelExists.getString(1);
      BCrypt.Result result = BCrypt.verifyer().verify(password.toCharArray(), dbPassword);
      if (result.verified) {
        PreparedStatement selSession = conn.prepareStatement("SELECT * FROM users WHERE session = ?");
        selSession.setString(1, session);
        ResultSet rsSession = selSession.executeQuery();
        if (rsSession.next()) {
          return false; // session key is not unique and already exists in database
        } else {
          PreparedStatement updData = conn.prepareStatement("UPDATE users SET session = ?");
          updData.setString(1, session);
          updData.executeUpdate();
          return true; // done
        }
      } else {
        return false; // password is wrong
      }
    } else {
      return false; // if user not exists
    }
  }

  public Achievement checkAchievement(String session) {
    // checks and returns achievement, if got
    // FEATURE: will be developed later
    return null;
  }

  public void changePassword(String session, String newPassword) throws SQLException {
    // sets new hashed password for user in database
    PreparedStatement updData = conn.prepareStatement("UPDATE users SET password = ? WHERE session = ?");
    String hashedPassword = BCrypt.withDefaults().hashToString(12, newPassword.toCharArray());
    updData.setString(1, hashedPassword);
    updData.setString(2, session);

    updData.executeUpdate();
  }

  public void changeProfilePic(String session, int picId) throws SQLException {
    // changes profile picture. default picture id is 0
    PreparedStatement updData = conn.prepareStatement("UPDATE users SET pic_id = ? WHERE session = ?");
    updData.setInt(1, picId);
    updData.setString(2, session);
    updData.executeUpdate();
  }

  public int getProfilePic(String session) throws SQLException {
    // gets profile picture id
    PreparedStatement selPic = conn.prepareStatement("SELECT pic_id FROM users WHERE session = ?");
    selPic.setString(1, session);
    ResultSet rsSession = selPic.executeQuery();
    if (rsSession.next()) {
      return rsSession.getInt(1); // picId
    } else {
      return 0; // if not exists
    }
  }

  public void changeDescription(String session, String description) throws SQLException {
    // changes description. default description is null
    PreparedStatement updData = conn.prepareStatement("UPDATE users SET description = ? WHERE session = ?");
    updData.setString(1, description);
    updData.setString(2, session);
    updData.executeUpdate();
  }

  public String getDescription(String session) throws SQLException {
    // gets profile picture id
    PreparedStatement selDescription = conn.prepareStatement("SELECT description FROM users WHERE session = ?");
    selDescription.setString(1, session);
    ResultSet rsDescription = selDescription.executeQuery();
    if (rsDescription.next()) {
      return rsDescription.getString(1); // description
    } else {
      return ""; // if not exists
    }
  }

  public void setLastActivity(String session, Instant time) throws SQLException {
    // sets last activity according to the last game played. used by addGamePlayed
    PreparedStatement updData = conn.prepareStatement("UPDATE users SET last_activity = ? WHERE session = ?");
    updData.setTimestamp(1, Timestamp.from(time));
    updData.setString(2, session);
    updData.executeUpdate();
  }

  public Timestamp getLastActivity(String session) throws SQLException {
    // gets last activity
    PreparedStatement selLastActivity = conn.prepareStatement("SELECT last_activity FROM users WHERE session = ?");
    selLastActivity.setString(1, session);
    ResultSet rsLastActivity = selLastActivity.executeQuery();
    if (rsLastActivity.next()) {
      return rsLastActivity.getTimestamp(1); // lastActivity timestamp
    } else {
      return null; // if not exists
    }
  }

  public void setCurrentGame(String session, int gameId) throws SQLException {
    // sets current game. used by createGame while creating, public used while joining
    PreparedStatement updData = conn.prepareStatement("UPDATE users SET current_game_id = ? WHERE session = ?");
    updData.setInt(1, gameId);
    updData.setString(2, session);
    updData.executeUpdate();
  }

  public Integer getCurrentGame(String session) throws SQLException {
    // gets current game
    PreparedStatement selCurrentGame = conn.prepareStatement("SELECT current_game_id FROM users WHERE session = ?");
    selCurrentGame.setString(1, session);
    ResultSet rsCurrentGame = selCurrentGame.executeQuery();
    if (rsCurrentGame.next()) {
      int res = rsCurrentGame.getInt(1);
      return res == 0 ? null : res; // gameId
    } else {
      return null; // if not exists
    }
  }

  public void addGamePlayed(String session) throws SQLException {
    // adds a new game played
    Integer gameId = getCurrentGame(session);
    Integer userId = getUserId(session);
    if (!(Objects.equals(gameId, null) || Objects.equals(userId, null))) {
      PreparedStatement updData = conn.prepareStatement("UPDATE users SET games_played_number = games_played_number + 1 WHERE session = ?; INSERT INTO games_history (game_id, user_id) VALUES (?, ?);");
      updData.setString(1, session);
      updData.setInt(2, gameId);
      updData.setInt(3, userId);

      updData.executeUpdate();
    }
  }

  public Integer[] getGamesPlayed(String session) throws SQLException {
    // collects all the data about user's game history
    Integer userId = getUserId(session);
    if (userId == null) {
      return null;
    }
    List<Integer> gameList = new ArrayList<>();
    PreparedStatement selGamesHistory = conn.prepareStatement("SELECT game_id FROM games_history WHERE user_id = ?");
    selGamesHistory.setInt(1, userId);
    ResultSet rsGamesHistory = selGamesHistory.executeQuery();
    while (rsGamesHistory.next()) {
      gameList.add(rsGamesHistory.getInt("game_id")); // array of gameIds
    }

    return gameList.isEmpty() ? null : gameList.toArray(new Integer[0]);
  }

  public void addGlobalPoints(String session, int points) throws SQLException {
    // adds global points to the user's statistics data
    PreparedStatement updData = conn.prepareStatement("UPDATE users SET global_points = global_points + ? WHERE session = ?");
    updData.setInt(1, points);
    updData.setString(2, session);
    updData.executeUpdate();
  }

  public Integer getGlobalPoints(String session) throws SQLException {
    // gets global points
    PreparedStatement selGlobalPoints = conn.prepareStatement("SELECT global_points FROM users WHERE session = ?");
    selGlobalPoints.setString(1, session);
    ResultSet rsGlobalPoints = selGlobalPoints.executeQuery();
    if (rsGlobalPoints.next()) {
      return rsGlobalPoints.getInt(1); // globalPoints
    } else {
      return null; // if not exists
    }
  }

  public void addGlobalPossiblePoints(String session, int possiblePoints) throws SQLException {
    // adds global possible points to the user's statistics data
    PreparedStatement updData = conn.prepareStatement("UPDATE users SET global_possible_points = global_possible_points + ? WHERE session = ?");
    updData.setInt(1, possiblePoints);
    updData.setString(2, session);
    updData.executeUpdate();
  }

  public Integer getGlobalPossiblePoints(String session) throws SQLException {
    // gets global possible points
    PreparedStatement selGlobalPossiblePoints = conn.prepareStatement("SELECT global_possible_points FROM users WHERE session = ?");
    selGlobalPossiblePoints.setString(1, session);
    ResultSet rsGlobalPossiblePoints = selGlobalPossiblePoints.executeQuery();
    if (rsGlobalPossiblePoints.next()) {
      return rsGlobalPossiblePoints.getInt(1); // globalPossiblePoints
    } else {
      return null; // if not exists
    }
  }

  public void addCurrentGamePoints(String session, int points) throws SQLException {
    // adds current game points to the user's statistics data
    PreparedStatement updData = conn.prepareStatement("UPDATE users SET current_game_points = current_game_points + ? WHERE session = ?");
    updData.setInt(1, points);
    updData.setString(2, session);
    updData.executeUpdate();
  }

  public Integer getCurrentGamePoints(String session) throws SQLException {
    // gets current game points
    PreparedStatement selCurrentGamePoints = conn.prepareStatement("SELECT current_game_points FROM users WHERE session = ?");
    selCurrentGamePoints.setString(1, session);
    ResultSet rsCurrentGamePoints = selCurrentGamePoints.executeQuery();
    if (rsCurrentGamePoints.next()) {
      return rsCurrentGamePoints.getInt(1); // currentGamePoints
    } else {
      return null; // if not exists
    }
  }

  public void logOut(String session) throws SQLException {
    // erases the session info in the users data
    // adds global points to the user's statistics data
    PreparedStatement updData = conn.prepareStatement("UPDATE users SET session = NULL WHERE session = ?");
    updData.setString(1, session);
    updData.executeUpdate();
  }

  //
  // Games TABLE REFERRED METHODS
  //

  public Integer createGame(int authorId, int levelDifficulty, int numberOfQuestions, int participantsNumber, int topicId) throws SQLException {
    // creates the game room, returns gameId
    // default isPrivate = true
    PreparedStatement inpGame = conn.prepareStatement("INSERT INTO games (status, author_id, created_at, is_private, level_difficulty, current_question_number, number_of_questions, participants_number, topic_id) VALUES (0, ?, ?, 1, ?, 0, ?, ?, ?", Statement.RETURN_GENERATED_KEYS);
    inpGame.setInt(1, authorId);
    inpGame.setTimestamp(2, Timestamp.from(Instant.now()));
    inpGame.setInt(3, levelDifficulty);
    inpGame.setInt(4, numberOfQuestions);
    inpGame.setInt(5, participantsNumber);
    inpGame.setInt(6, topicId);

    inpGame.executeUpdate();
    ResultSet rsInpGame = inpGame.getGeneratedKeys();
    return rsInpGame.getInt(1); // gameId
  }

  public Boolean checkGameExists(int gameId) throws SQLException {
    // checks if game exists or not
    PreparedStatement selExists = conn.prepareStatement("SELECT * FROM games WHERE id = ?");
    selExists.setInt(1, gameId);
    ResultSet rsSelExists = selExists.executeQuery();
    return rsSelExists.isBeforeFirst(); // true if game exists, false if not exists
  }

  public Integer[] getPreset(int gameId) throws SQLException {
    // gets the preset of the game (authorId, levelDifficulty, numberOfQuestions, participantsNumber, topicId)
    PreparedStatement selPreset = conn.prepareStatement("SELECT author_id, level_difficulty, number_of_questions, participants_number, topic_id FROM games WHERE id = ?");
    selPreset.setInt(1, gameId);
    ResultSet rsPreset = selPreset.executeQuery();
    if (rsPreset.next()) {
      return (Integer[]) rsPreset.getArray(1).getArray(); // timestamp
    } else {
      return null; // if not exists
    }
  }

  private void setStatus(int gameId, int status) throws SQLException {
    // sets status for the game
    // 0 - pending, 1 - paused, 2 - active, 3 - ended
    PreparedStatement updData = conn.prepareStatement("UPDATE games SET status = ? WHERE id = ?");
    updData.setInt(1, status);
    updData.setInt(2, gameId);
    updData.executeUpdate();
  }

  public Integer getStatus(int gameId) throws SQLException {
    // gets status for the game
    PreparedStatement selStatus = conn.prepareStatement("SELECT status FROM games WHERE id = ?");
    selStatus.setInt(1, gameId);
    ResultSet rsStatus = selStatus.executeQuery();
    if (rsStatus.next()) {
      return rsStatus.getInt(1); // status: 0 - pending, 1 - paused, 2 - active, 3 - ended
    } else {
      return null; // if not exists
    }
  }

  public void setGameStartTime(int gameId, Instant gameStartTime) throws SQLException {
    // sets the time of starting for the game
    PreparedStatement updData = conn.prepareStatement("UPDATE games SET game_start_time = ? WHERE id = ?");
    updData.setTimestamp(1, Timestamp.from(gameStartTime));
    updData.setInt(2, gameId);
    updData.executeUpdate();
  }

  public Timestamp getGameStartTime(int gameId) throws SQLException {
    // gets time of start of the game
    PreparedStatement selTime = conn.prepareStatement("SELECT game_start_time FROM games WHERE id = ?");
    selTime.setInt(1, gameId);
    ResultSet rsTime = selTime.executeQuery();
    if (rsTime.next()) {
      return rsTime.getTimestamp(1); // timestamp
    } else {
      return null; // if not exists
    }
  }

  public void setPrivate(int gameId, boolean isPrivate) throws SQLException {
    // sets the private option for the game
    // sets the time of creating for the game
    PreparedStatement updData = conn.prepareStatement("UPDATE games SET isPrivate = ? WHERE id = ?");
    updData.setBoolean(1, isPrivate);
    updData.setInt(2, gameId);
    updData.executeUpdate();
  }

  public Boolean getPrivate(int gameId) throws SQLException {
    // gets the privateness option of the game
    PreparedStatement selPrivate = conn.prepareStatement("SELECT is_private FROM games WHERE id = ?");
    selPrivate.setInt(1, gameId);
    ResultSet rsTime = selPrivate.executeQuery();
    if (rsTime.next()) {
      return rsTime.getBoolean(1); // true - is private, false - is not
    } else {
      return null; // if not exists
    }
  }

  private void setGameEndTime(int gameId, Instant gameEndTime) throws SQLException {
    // sets the time of ending for the game
    PreparedStatement updData = conn.prepareStatement("UPDATE games SET game_end_time = ? WHERE id = ?");
    updData.setTimestamp(1, Timestamp.from(gameEndTime));
    updData.setInt(2, gameId);
    updData.executeUpdate();
  }

  public Timestamp getGameEndTime(int gameId) throws SQLException {
    // gets time of end of the game
    PreparedStatement selTime = conn.prepareStatement("SELECT game_end_time FROM games WHERE id = ?");
    selTime.setInt(1, gameId);
    ResultSet rsTime = selTime.executeQuery();
    if (rsTime.next()) {
      return rsTime.getTimestamp(1); // timestamp
    } else {
      return null; // if not exists
    }
  }

  public void stopGame(int gameId) throws SQLException {
    // stops the game in Games table, must be used with addGamePlayed
    setStatus(gameId, 3);
    setGameEndTime(gameId, Instant.now());

    PreparedStatement updData = conn.prepareStatement("UPDATE users SET current_game_id = NULL WHERE current_game_id = ?");
    updData.setInt(1, gameId);
    updData.executeUpdate();
  }

  public void deleteGame(int gameId) throws SQLException {
    // deletes the game
    PreparedStatement updData = conn.prepareStatement("DELETE FROM games WHERE id = ?");
    updData.setInt(1, gameId);
    updData.executeUpdate();
  }

  public Integer getCurrentQuestionNumber(int gameId) throws SQLException {
    // gets the current question number of the game
    PreparedStatement selCurrQuestion = conn.prepareStatement("SELECT game_end_time FROM games WHERE id = ?");
    selCurrQuestion.setInt(1, gameId);
    ResultSet rsCurrQuestion = selCurrQuestion.executeQuery();
    if (rsCurrQuestion.next()) {
      return rsCurrQuestion.getInt(1); // currentQuestionNumber
    } else {
      return null; // if not exists
    }
  }

  public Question nextQuestion(int gameId) throws SQLException {
    // returns a new object of next question

    PreparedStatement updData = conn.prepareStatement("UPDATE games SET current_question_number = current_question_number + 1 WHERE id = ?");
    updData.setInt(1, gameId);
    updData.executeUpdate();

    PreparedStatement selCurrentNum = conn.prepareStatement("SELECT current_question_number FROM games WHERE id = ?");
    selCurrentNum.setInt(1, gameId);
    ResultSet rsCurrentNum = selCurrentNum.executeQuery();
    int currentNum;
    if (rsCurrentNum.next()) {
      currentNum = rsCurrentNum.getInt(1);
    } else {
      return null; // not found
    }

    PreparedStatement selQuestion = conn.prepareStatement("SELECT question_text, answer1, answer2, answer3, answer4 FROM questions WHERE question_number = ? AND game_id = ?");
    selQuestion.setInt(1, currentNum);
    selQuestion.setInt(2, gameId);

    ResultSet rsQuestion = selQuestion.executeQuery();
    if (rsQuestion.next()) {
      Question questionObj = new Question();
      questionObj.questionText = rsQuestion.getString(1);
      questionObj.answer1 = rsQuestion.getString(2);
      questionObj.answer2 = rsQuestion.getString(3);
      questionObj.answer3 = rsQuestion.getString(4);
      questionObj.answer4 = rsQuestion.getString(5);

      return questionObj; // Question
    } else {
      return null; // not found
    }
  }

  public boolean validateAnswer(int gameId, int questionNumber, int submittedAnswerIndex) throws SQLException {
    // validates submitted answer
    PreparedStatement selRightAnswer = conn.prepareStatement("SELECT right_answer_index FROM questions WHERE question_number = ? AND game_id = ?");
    selRightAnswer.setInt(1, questionNumber);
    selRightAnswer.setInt(2, gameId);
    ResultSet rsRightAnswer = selRightAnswer.executeQuery();
    if (rsRightAnswer.next()) {
      return submittedAnswerIndex == rsRightAnswer.getInt(1); // true if right, false if not
    } else {
      return false; // not found
    }
  }

  public void loadQuestions(int gameId, JSONArray jsonArr) throws SQLException {
    // example of json argument is described in the documentation catalog
    // loads questions data from json and commits it into the database
    final int ANSWER_AMOUNT = 4;

    for (int i = 0; i < jsonArr.length(); i++) {
      JSONObject itemObject = jsonArr.getJSONObject(i);
      int questionNumber = itemObject.getInt("question_number");
      String questionText = itemObject.getString("question_text");
      int rightAnswerNumber = itemObject.getInt("right_answer_number");
      PreparedStatement inpQuestion = conn.prepareStatement("INSERT INTO questions (game_id, question_number, question_text, right_answer_number, answer1, answer2, answer3, answer4) VALUES (?, ?, ?, ?, ?, ?, ?, ?)");
      inpQuestion.setInt(1, gameId);
      inpQuestion.setInt(2, questionNumber);
      inpQuestion.setString(3, questionText);
      inpQuestion.setInt(4, rightAnswerNumber);

      JSONArray availableAnswersArr = itemObject.getJSONArray("available_answers");
      for (int j = 0; j < ANSWER_AMOUNT; j++) {
        JSONObject answerObj = availableAnswersArr.getJSONObject(j);
        int answerIndex = answerObj.getInt("index");
        String answer = answerObj.getString("answer");
        inpQuestion.setString(4 + answerIndex, answer);
      }
      inpQuestion.executeUpdate();
    }
  }

  //
  // Achievements TABLE REFERRED METHODS
  //

  public Integer addAchievement(Achievement achievement) throws SQLException {
    // creates a new achievement, returns id of it
    // FEATURE
    return null;
  }

  public void removeAchievement(Achievement achievement) {
    // removes an existing achievement
    // FEATURE
  }

  //
  // Topics TABLE REFERRED METHODS
  //

  public Integer addTopic(Topic topic) {
    // creates a new topic
    // FEATURE
    return null;
  }

  public void removeTopic(Topic topic) {
    // removes an existing topic
    // FEATURE
  }
}