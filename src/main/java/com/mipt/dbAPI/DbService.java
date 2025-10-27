package com.mipt.dbAPI;

import java.sql.*;
import at.favre.lib.crypto.bcrypt.BCrypt;
import com.mipt.domainModel.*;
import org.json.JSONObject;
import org.json.JSONArray;
import java.time.Instant;


public class DbService {
  private static Connection connect() {
    // establishes connection to the database
    String url = "jdbc:postgresql://localhost:5432/my";
    String user = "postgres";
    String password = "postgres";

    try {
      return DriverManager.getConnection(url, user, password);
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  private static final Connection conn = connect();

  public static class DbUser {
    public static Integer checkLoggedIn(String session) throws SQLException {
      // must be completed every query to check user authenticated to perform actions
      PreparedStatement selId = conn.prepareStatement("SELECT id FROM users WHERE session = ?");
      selId.setString(1, session);

      ResultSet rsSelId = selId.executeQuery();
      if (rsSelId.next()) {
        return !rsSelId.isBeforeFirst() ? rsSelId.getInt(1) : null; // returns userId if logged in, null if not logged
      } else {
        return null; // null if session is not exists
      }
    }

    public static boolean checkUserExists(String username) throws SQLException {
      PreparedStatement selExists = conn.prepareStatement("SELECT * FROM users WHERE username = ?");
      selExists.setString(1, username);
      ResultSet rsSelExists = selExists.executeQuery();
      return rsSelExists.isBeforeFirst(); // true if user already exists, false if not exists
    }

    public static boolean register(String username, String password) throws SQLException {
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

    public static boolean authorize(String username, String password, String session) throws SQLException {
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

    public static Achievement checkAchievement(String session) {
      // checks and returns achievement, if got
      // FEATURE: will be developed later
      return null;
    }

    public static void changePassword(String session, String newPassword) throws SQLException {
      // sets new hashed password for user in database
      PreparedStatement updData = conn.prepareStatement("UPDATE users SET password = ? WHERE session = ?");
      String hashedPassword = BCrypt.withDefaults().hashToString(12, newPassword.toCharArray());
      updData.setString(1, hashedPassword);
      updData.setString(2, session);

      updData.executeUpdate();
    }

    public static void changeProfilePic(String session, int picId) throws SQLException {
      // changes profile picture. default picture id is 0
      PreparedStatement updData = conn.prepareStatement("UPDATE users SET pic_id = ? WHERE session = ?");
      updData.setInt(1, picId);
      updData.setString(2, session);
      updData.executeUpdate();
    }

    public static int getProfilePic(String session) throws SQLException {
      // gets profile picture id
      PreparedStatement selPic = conn.prepareStatement("SELECT pic_id FROM users WHERE session = ?");
      selPic.setString(1, session);
      ResultSet rsSession = selPic.executeQuery();
      if (rsSession.next()) {
        return rsSession.getInt(1);
      } else {
        return 0;
      }
    }

    public static void changeDescription(String session, String description) throws SQLException {
      // changes description. default description is null
      PreparedStatement updData = conn.prepareStatement("UPDATE users SET description = ? WHERE session = ?");
      updData.setString(1, description);
      updData.setString(2, session);
      updData.executeUpdate();
    }

    public static String getDescription(String session) throws SQLException {
      // gets profile picture id
      PreparedStatement selDescription = conn.prepareStatement("SELECT description FROM users WHERE session = ?");
      selDescription.setString(1, session);
      ResultSet rsDescription = selDescription.executeQuery();
      if (rsDescription.next()) {
        return rsDescription.getString(1);
      } else {
        return "";
      }
    }

    public static void setLastActivity(String session, Instant time) throws SQLException {
      // sets last activity according to the last game played. used by addGamePlayed
      PreparedStatement updData = conn.prepareStatement("UPDATE users SET last_activity = ? WHERE session = ?");
      updData.setTimestamp(1, Timestamp.from(time));
      updData.setString(2, session);
      updData.executeUpdate();
    }

    public static Timestamp getLastActivity(String session) throws SQLException {
      // gets last activity
      PreparedStatement selLastActivity = conn.prepareStatement("SELECT last_activity FROM users WHERE session = ?");
      selLastActivity.setString(1, session);
      ResultSet rsLastActivity = selLastActivity.executeQuery();
      if (rsLastActivity.next()) {
        return rsLastActivity.getTimestamp(1);
      } else {
        return null;
      }
    }

    public static void setCurrentGame(String session, int gameId) throws SQLException {
      // sets current game. used by DbGame.createGame while creating, public used while joining
      PreparedStatement updData = conn.prepareStatement("UPDATE users SET current_game_id = ? WHERE session = ?");
      updData.setInt(1, gameId);
      updData.setString(2, session);
      updData.executeUpdate();
    }

    public static Integer getCurrentGame(String session) throws SQLException {
      // gets current game
      PreparedStatement selCurrentGame = conn.prepareStatement("SELECT current_game_id FROM users WHERE session = ?");
      selCurrentGame.setString(1, session);
      ResultSet rsCurrentGame = selCurrentGame.executeQuery();
      if (rsCurrentGame.next()) {
        int currentGameId = rsCurrentGame.getInt(1);
        if (currentGameId == 0) {
          return null;
        } else {
          return currentGameId;
        }
      } else {
        return null;
      }
    }

    public static void addGamePlayed(String session, Game game) throws SQLException {
      // adds a new game played
      PreparedStatement updData = conn.prepareStatement("UPDATE users SET games_played_number = games_played_number + 1 WHERE session = ?");
      updData.setString(1, session);
      updData.executeUpdate();
      // FEATURE: the structure will be reformatted later (JSON games_played_ids, achievement_ids -> TABLES)
    }

    public static void getGamesPlayed(String session) throws SQLException {
      // FEATURE
    }

    public static void addGlobalPoints(String session, int points) throws SQLException {
      // adds global points to the user's statistics data
      PreparedStatement updData = conn.prepareStatement("UPDATE users SET global_points = global_points + ? WHERE session = ?");
      updData.setInt(1, points);
      updData.setString(2, session);
      updData.executeUpdate();
    }

    public static Integer getGlobalPoints(String session) throws SQLException {
      // gets global points
      PreparedStatement selGlobalPoints = conn.prepareStatement("SELECT global_points FROM users WHERE session = ?");
      selGlobalPoints.setString(1, session);
      ResultSet rsGlobalPoints = selGlobalPoints.executeQuery();
      if (rsGlobalPoints.next()) {
        return rsGlobalPoints.getInt(1);
      } else {
        return null;
      }
    }

    public static void addGlobalPossiblePoints(String session, int possiblePoints) throws SQLException {
      // adds global possible points to the user's statistics data
      PreparedStatement updData = conn.prepareStatement("UPDATE users SET global_possible_points = global_possible_points + ? WHERE session = ?");
      updData.setInt(1, possiblePoints);
      updData.setString(2, session);
      updData.executeUpdate();
    }

    public static Integer getGlobalPossiblePoints(String session) throws SQLException {
      // gets global possible points
      PreparedStatement selGlobalPossiblePoints = conn.prepareStatement("SELECT global_possible_points FROM users WHERE session = ?");
      selGlobalPossiblePoints.setString(1, session);
      ResultSet rsGlobalPossiblePoints = selGlobalPossiblePoints.executeQuery();
      if (rsGlobalPossiblePoints.next()) {
        return rsGlobalPossiblePoints.getInt(1);
      } else {
        return null;
      }
    }

    public static void addCurrentGamePoints(String session, int points) throws SQLException {
      // adds current game points to the user's statistics data
      PreparedStatement updData = conn.prepareStatement("UPDATE users SET current_game_points = current_game_points + ? WHERE session = ?");
      updData.setInt(1, points);
      updData.setString(2, session);
      updData.executeUpdate();
    }

    public static Integer getCurrentGamePoints(String session) throws SQLException {
      // gets current game points
      PreparedStatement selCurrentGamePoints = conn.prepareStatement("SELECT current_game_points FROM users WHERE session = ?");
      selCurrentGamePoints.setString(1, session);
      ResultSet rsCurrentGamePoints = selCurrentGamePoints.executeQuery();
      if (rsCurrentGamePoints.next()) {
        return rsCurrentGamePoints.getInt(1);
      } else {
        return null;
      }
    }

    public static void logOut(String session) throws SQLException {
      // erases the session info in the users data
      // adds global points to the user's statistics data
      PreparedStatement updData = conn.prepareStatement("UPDATE users SET session = NULL WHERE session = ?");
      updData.setString(1, session);
      updData.executeUpdate();
    }
  }

  public static class DbGame {
    public static Integer createGame(int authorId, int levelDifficulty, int numberOfQuestions, int participantsNumber, int topicId) throws SQLException {
      // creates the game room, returns gameId
      // needs author, topic, numberOfQuestions, levelDifficulty, participantsNumber to initialize
      // default is_private = true
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

    public static void checkGameExists(int gameId) {
      // FEATURE
    }

    private static void setStatus(int gameId, int status) throws SQLException {
      // sets status for the game
      // 0 - pending, 1 - paused, 2 - active, 3 - ended
      PreparedStatement updData = conn.prepareStatement("UPDATE games SET status = ? WHERE id = ?");
      updData.setInt(1, status);
      updData.setInt(2, gameId);
      updData.executeUpdate();
    }

    public static void getStatus(int gameId) {
      // FEATURE
    }

    private static void setGameStartTime(int gameId, Instant gameStartTime) throws SQLException {
      // sets the time of starting for the game
      PreparedStatement updData = conn.prepareStatement("UPDATE games SET game_start_time = ? WHERE id = ?");
      updData.setTimestamp(1, Timestamp.from(gameStartTime));
      updData.setInt(2, gameId);
      updData.executeUpdate();
    }

    public static void getGameStartTime(int gameId) {
      // FEATURE
    }

    public static void setPrivate(int gameId, boolean isPrivate) throws SQLException {
      // sets the private option for the game
      // sets the time of creating for the game
      PreparedStatement updData = conn.prepareStatement("UPDATE games SET isPrivate = ? WHERE id = ?");
      updData.setBoolean(1, isPrivate);
      updData.setInt(2, gameId);
      updData.executeUpdate();
    }

    public static void getPrivate(int gameId) {
      // FEATURE
    }

    private static void setGameEndTime(int gameId, Instant gameEndTime) throws SQLException {
      // sets the time of ending for the game
      PreparedStatement updData = conn.prepareStatement("UPDATE games SET game_end_time = ? WHERE id = ?");
      updData.setTimestamp(1, Timestamp.from(gameEndTime));
      updData.setInt(2, gameId);
      updData.executeUpdate();
    }

    public static void getGameEndTime(int gameId) {
      // FEATURE
    }

    public static void startGame(int gameId) throws SQLException {
      // starts the game
      setStatus(gameId, 2);
      setGameStartTime(gameId, Instant.now());
    }

    public static void pauseGame(int gameId) throws SQLException {
      // pauses the game
      setStatus(gameId, 1);
    }

    public static void resumeGame(int gameId) throws SQLException {
      // pauses the game
      setStatus(gameId, 2);
    }

    public static void stopGame(int gameId) throws SQLException {
      // stops the game in Games table, must be used with DbUser.addGamePlayed
      setStatus(gameId, 3);
      setGameEndTime(gameId, Instant.now());

      PreparedStatement updData = conn.prepareStatement("UPDATE users SET current_game_id = NULL WHERE current_game_id = ?");
      updData.setInt(1, gameId);
      updData.executeUpdate();
    }

    public static void deleteGame(int gameId) throws SQLException {
      // deletes the game
      PreparedStatement updData = conn.prepareStatement("DELETE FROM games WHERE id = ?");
      updData.setInt(1, gameId);
      updData.executeUpdate();
    }

    public static Question nextQuestion(int gameId) throws SQLException {
      // returns a new object of next question
      // FIX
      PreparedStatement updData = conn.prepareStatement("UPDATE games SET current_question_number = current_question_number + 1 WHERE id = ?");
      updData.setInt(1, gameId);
      updData.executeUpdate();

      PreparedStatement selCurrentNum = conn.prepareStatement("SELECT current_question_number FROM games WHERE id = ?");
      selCurrentNum.setInt(1, gameId);
      ResultSet rsCurrentNum = selCurrentNum.executeQuery();
      int currentNum = rsCurrentNum.getInt(1);

      PreparedStatement selQuestion = conn.prepareStatement("SELECT question_text, answer1, answer2, answer3, answer4 FROM questions WHERE question_number = ? AND game_id = ?");
      selQuestion.setInt(1, currentNum);
      selQuestion.setInt(2, gameId);

      ResultSet rsQuestion = selQuestion.executeQuery();
      Question questionObj = new Question();

      questionObj.questionText = rsQuestion.getString(1);
      questionObj.answer1 = rsQuestion.getString(2);
      questionObj.answer2 = rsQuestion.getString(3);
      questionObj.answer3 = rsQuestion.getString(4);
      questionObj.answer4 = rsQuestion.getString(5);

      return questionObj;
    }

    public static boolean validateAnswer(int gameId, int questionNumber, int submittedAnswerIndex) throws SQLException {
      // validates submitted answer
      // FIX
      PreparedStatement selRightAnswer = conn.prepareStatement("SELECT right_answer_index FROM questions WHERE question_number = ? AND game_id = ?");
      selRightAnswer.setInt(1, questionNumber);
      selRightAnswer.setInt(2, gameId);
      ResultSet rsRightAnswer = selRightAnswer.executeQuery();
      return submittedAnswerIndex == rsRightAnswer.getInt(1);
    }

    public static void loadQuestions(int gameId, JSONArray jsonArr) throws SQLException {
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
  }

  public static class DbAchievement {
    public static Integer add(Achievement achievement) throws SQLException {
      // creates a new achievement, returns id of it
      // FEATURE
      return null;
    }

    public static void remove(Achievement achievement) {
      // removes an existing achievement
      // FEATURE
    }
  }

  public static class DbTopic {
    public static Integer add(Topic topic) {
      // creates a new topic
      // FEATURE
      return null;
    }

    public static void remove(Topic topic) {
      // removes an existing topic
      // FEATURE
    }
  }
}
