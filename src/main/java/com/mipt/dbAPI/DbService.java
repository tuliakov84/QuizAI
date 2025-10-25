package com.mipt.dbAPI;

import java.sql.*;
import at.favre.lib.crypto.bcrypt.BCrypt;
import com.mipt.domainModel.*;
import org.json.JSONObject;
import java.time.Instant;


public class DbService {
  private Connection connect() {
    // establishes connection to the database
    String url = "jdbc:postgresql://localhost:5432/my";
    String user = "postgresql";
    String password = "postgresql";

    try {
      return DriverManager.getConnection(url, user, password);
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  private final Connection conn = connect();

  public class DbUser {
    public Integer checkLoggedIn(String session) throws SQLException {
      // must be completed every query to check user authenticated to perform actions
      PreparedStatement selId = conn.prepareStatement("SELECT id FROM users WHERE session = ?");
      selId.setString(1, session);
      ResultSet rsSelId = selId.executeQuery();
      return rsSelId.getInt(1); // returns userId if logged in, null if not logged
    }

    private boolean checkUserExists(String username) throws SQLException {
      PreparedStatement selExists = conn.prepareStatement("SELECT * FROM users WHERE username = ?");
      selExists.setString(1, username);
      ResultSet rsSelExists = selExists.executeQuery();
      return rsSelExists != null; // true if user already exists, false if not exists
    }

    public boolean register(String username, String password) throws SQLException {
      // creates an account for a new user
      if (checkUserExists(username)) {
        return false; // false if user already exists
      }
      String hashedPassword = BCrypt.withDefaults().hashToString(12, password.toCharArray());
      PreparedStatement inpData = conn.prepareStatement("INSERT INTO users (username, password, pic_id) VALUES (?, ?, 0)");
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
      if (rsSelExists == null) {
        return false; // if user not exists
      }
      BCrypt.Result result = BCrypt.verifyer().verify(password.toCharArray(), rsSelExists.getString(1));
      if (result.verified) {
        PreparedStatement updData = conn.prepareStatement("UPDATE users SET session = ?");
        updData.setString(1, session);
        updData.executeUpdate();
        return true; // done
      } else {
        return false; // password is wrong
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

    public void changeDescription(String session, String description) throws SQLException {
      // changes description. default description is null
      PreparedStatement updData = conn.prepareStatement("UPDATE users SET description = ? WHERE session = ?");
      updData.setString(1, description);
      updData.setString(2, session);
      updData.executeUpdate();
    }

    private void setLastActivity(String session, Instant time) throws SQLException {
      // sets last activity according to the last game played. used by addGamePlayed
      PreparedStatement updData = conn.prepareStatement("UPDATE users SET last_activity = ? WHERE session = ?");
      updData.setTimestamp(1, Timestamp.from(time));
      updData.setString(2, session);
      updData.executeUpdate();
    }

    public void setCurrentGame(String session, int gameId) throws SQLException {
      // sets current game. used by DbGame.createGame while creating, public used while joining
      PreparedStatement updData = conn.prepareStatement("UPDATE users SET current_game_id = ? WHERE session = ?");
      updData.setInt(1, gameId);
      updData.setString(2, session);
      updData.executeUpdate();
    }

    public void addGamePlayed(String session, Game game) throws SQLException {
      // adds a new game played
      PreparedStatement updData = conn.prepareStatement("UPDATE users SET games_played_number = games_played_number + 1 WHERE session = ?");
      updData.setString(1, session);
      updData.executeUpdate();
      // FEATURE: the structure will be reformatted later (JSON games_played_ids, achievement_ids -> TABLES)
    }

    public void addGlobalPoints(String session, int points) throws SQLException {
      // adds global points to the user's statistics data
      PreparedStatement updData = conn.prepareStatement("UPDATE users SET global_points = global_points + ? WHERE session = ?");
      updData.setInt(1, points);
      updData.setString(2, session);
      updData.executeUpdate();
    }

    public void addGlobalPossiblePoints(String session, int possiblePoints) throws SQLException {
      // adds global possible points to the user's statistics data
      PreparedStatement updData = conn.prepareStatement("UPDATE users SET global_possible_points = global_possible_points + ? WHERE session = ?");
      updData.setInt(1, possiblePoints);
      updData.setString(2, session);
      updData.executeUpdate();
    }

    public void addCurrentGamePoints(String session, int points) throws SQLException {
      // adds current game points to the user's statistics data
      PreparedStatement updData = conn.prepareStatement("UPDATE users SET current_game_points = current_game_points + ? WHERE session = ?");
      updData.setInt(1, points);
      updData.setString(2, session);
      updData.executeUpdate();
    }

    public void logOut(String session) throws SQLException {
      // erases the session info in the users data
      // adds global points to the user's statistics data
      PreparedStatement updData = conn.prepareStatement("UPDATE users SET session = NULL WHERE session = ?");
      updData.setString(1, session);
      updData.executeUpdate();
    }
  }

  public class DbGame {
    public Integer createGame(int authorId, int levelDifficulty, int numberOfQuestions, int participantsNumber, int topicId) throws SQLException {
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

    private void setStatus(int gameId, int status) throws SQLException {
      // sets status for the game
      // 0 - pending, 1 - paused, 2 - active, 3 - ended
      PreparedStatement updData = conn.prepareStatement("UPDATE games SET status = ? WHERE id = ?");
      updData.setInt(1, status);
      updData.setInt(2, gameId);
      updData.executeUpdate();
    }

    private void setGameStartTime(int gameId, Instant gameStartTime) throws SQLException {
      // sets the time of starting for the game
      PreparedStatement updData = conn.prepareStatement("UPDATE games SET game_start_time = ? WHERE id = ?");
      updData.setTimestamp(1, Timestamp.from(gameStartTime));
      updData.setInt(2, gameId);
      updData.executeUpdate();
    }

    public void setPrivate(int gameId, boolean isPrivate) throws SQLException {
      // sets the private option for the game
      // sets the time of creating for the game
      PreparedStatement updData = conn.prepareStatement("UPDATE games SET isPrivate = ? WHERE id = ?");
      updData.setBoolean(1, isPrivate);
      updData.setInt(2, gameId);
      updData.executeUpdate();
    }

    private void setGameEndTime(int gameId, Instant gameEndTime) throws SQLException {
      // sets the time of ending for the game
      PreparedStatement updData = conn.prepareStatement("UPDATE games SET game_end_time = ? WHERE id = ?");
      updData.setTimestamp(1, Timestamp.from(gameEndTime));
      updData.setInt(2, gameId);
      updData.executeUpdate();
    }

    public void startGame(int gameId) throws SQLException {
      // starts the game
      setStatus(gameId, 2);
      setGameStartTime(gameId, Instant.now());
    }

    public void pauseGame(int gameId) throws SQLException {
      // pauses the game
      setStatus(gameId, 1);
    }

    public void resumeGame(int gameId) throws SQLException {
      // pauses the game
      setStatus(gameId, 2);
    }

    public void stopGame(int gameId) throws SQLException {
      // stops the game in Games table, must be used with DbUser.addGamePlayed
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

    public Question nextQuestion(int gameId) throws SQLException {
      // returns a new object of next question
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

    public boolean validateAnswer(int gameId, int questionNumber, int submittedAnswerIndex) throws SQLException {
      // validates submitted answer
      PreparedStatement selRightAnswer = conn.prepareStatement("SELECT right_answer_index FROM questions WHERE question_number = ? AND game_id = ?");
      selRightAnswer.setInt(1, questionNumber);
      selRightAnswer.setInt(2, gameId);
      ResultSet rsRightAnswer = selRightAnswer.executeQuery();
      return submittedAnswerIndex == rsRightAnswer.getInt(1);
    }

    public void loadQuestions(JSONObject json) {
      // example of json argument is described in the documentation catalog
      // FEATURE: will be developed later
    }
  }

  public class DbAchievement {
    public void add(Achievement achievement) {
      // creates a new achievement
      // FEATURE
    }

    public void remove(Achievement achievement) {
      // removes an existing achievement
      // FEATURE
    }
  }

  public class DbTopic {
    public void add(Topic topic) {
      // creates a new topic
      // FEATURE
    }

    public void remove(Topic topic) {
      // removes an existing topic
      // FEATURE
    }
  }
}
