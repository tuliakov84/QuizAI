package com.mipt.dbAPI;

import java.sql.*;
import at.favre.lib.crypto.bcrypt.BCrypt;
import com.mipt.domainModel.*;
import org.json.JSONObject;
import org.json.JSONArray;
import java.time.Instant;
import java.util.*;


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

  public void register(String username, String password) throws SQLException, DatabaseAccessException {
    // creates an account for a new user
    
    if (checkUserExists(username)) {
      throw new DatabaseAccessException("User already exists"); // if user already exists
    }
    String hashedPassword = BCrypt.withDefaults().hashToString(12, password.toCharArray());
    PreparedStatement inpData = conn.prepareStatement("INSERT INTO users" +
      " (username, password, pic_id, description, games_played_number, global_points, global_possible_points, current_game_points)" +
      " VALUES (?, ?, 0, '', 0, 0, 0, 0)");
    inpData.setString(1, username);
    inpData.setString(2, hashedPassword);
    inpData.executeUpdate();
  }

  public void authenticate(String username, String password, String session) throws SQLException, DatabaseAccessException {
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
          throw new DatabaseAccessException("Session key is not unique and already exists in database");
        } else {
          PreparedStatement updData = conn.prepareStatement("UPDATE users SET session = ? WHERE username = ?");
          updData.setString(1, session);
          updData.setString(2, username);
          updData.executeUpdate();
        }
      } else {
        throw new DatabaseAccessException("Wrong password"); // password is wrong
      }
    } else {
      throw new DatabaseAccessException(); // if user not exists
    }
  }

  public void changePassword(String session, String newPassword) throws SQLException, DatabaseAccessException {
    // sets new hashed password for user in database

    Integer userId = getUserId(session);
    if (userId == null) {
      throw new DatabaseAccessException(); // if there is no such user
    }
    PreparedStatement updData = conn.prepareStatement("UPDATE users SET password = ? WHERE session = ?");
    String hashedPassword = BCrypt.withDefaults().hashToString(12, newPassword.toCharArray());
    updData.setString(1, hashedPassword);
    updData.setString(2, session);

    updData.executeUpdate();
  }

  public void changeProfilePic(String session, int picId) throws SQLException, DatabaseAccessException {
    // changes profile picture. default picture id is 0

    Integer userId = getUserId(session);
    if (userId == null) {
      throw new DatabaseAccessException(); // if there is no such user
    }
    PreparedStatement updData = conn.prepareStatement("UPDATE users SET pic_id = ? WHERE session = ?");
    updData.setInt(1, picId);
    updData.setString(2, session);
    updData.executeUpdate();
  }

  public int getProfilePic(String session) throws SQLException, DatabaseAccessException {
    // gets profile picture id
    
    PreparedStatement selPic = conn.prepareStatement("SELECT pic_id FROM users WHERE session = ?");
    selPic.setString(1, session);
    ResultSet rsSession = selPic.executeQuery();
    if (rsSession.next()) {
      return rsSession.getInt(1); // picId
    } else {
      throw new DatabaseAccessException(); // if not exists
    }
  }

  public String getUsername(int userId) throws SQLException, DatabaseAccessException {
    // gets username for userId

    PreparedStatement selUsers = conn.prepareStatement("SELECT username FROM users WHERE id = ?");
    selUsers.setInt(1, userId);
    ResultSet rsUsers = selUsers.executeQuery();
    if (rsUsers.next()) {
      return rsUsers.getString(1);
    } else {
      throw new DatabaseAccessException(); // not found
    }
  }

  public String getUsername(String session) throws SQLException, DatabaseAccessException {
    // gets username for session

    PreparedStatement selUsers = conn.prepareStatement("SELECT username FROM users WHERE session = ?");
    selUsers.setString(1, session);
    ResultSet rsUsers = selUsers.executeQuery();
    if (rsUsers.next()) {
      return rsUsers.getString(1);
    } else {
      throw new DatabaseAccessException(); // not found
    }
  }

  public void changeDescription(String session, String description) throws SQLException, DatabaseAccessException {
    // changes description. default description is null

    Integer userId = getUserId(session);
    if (userId == null) {
      throw new DatabaseAccessException(); // if there is no such user
    }
    PreparedStatement updData = conn.prepareStatement("UPDATE users SET description = ? WHERE session = ?");
    updData.setString(1, description);
    updData.setString(2, session);
    updData.executeUpdate();
  }

  public String getDescription(String session) throws SQLException, DatabaseAccessException {
    // gets profile picture id
    
    PreparedStatement selDescription = conn.prepareStatement("SELECT description FROM users WHERE session = ?");
    selDescription.setString(1, session);
    ResultSet rsDescription = selDescription.executeQuery();
    if (rsDescription.next()) {
      return rsDescription.getString(1); // description
    } else {
      throw new DatabaseAccessException(); // if not exists
    }
  }

  public void setLastActivity(String session, Instant time) throws SQLException, DatabaseAccessException {
    // sets last activity according to the last game played. used by addGamePlayed

    Integer userId = getUserId(session);
    if (userId == null) {
      throw new DatabaseAccessException(); // if there is no such user
    }
    PreparedStatement updData = conn.prepareStatement("UPDATE users SET last_activity = ? WHERE session = ?");
    updData.setTimestamp(1, Timestamp.from(time));
    updData.setString(2, session);
    updData.executeUpdate();
  }

  public Timestamp getLastActivity(String session) throws SQLException, DatabaseAccessException {
    // gets last activity
    
    PreparedStatement selLastActivity = conn.prepareStatement("SELECT last_activity FROM users WHERE session = ?");
    selLastActivity.setString(1, session);
    ResultSet rsLastActivity = selLastActivity.executeQuery();
    if (rsLastActivity.next()) {
      return rsLastActivity.getTimestamp(1); // lastActivity timestamp
    } else {
      throw new DatabaseAccessException(); // if not exists
    }
  }

  public void setCurrentGame(String session, Integer gameId) throws SQLException, DatabaseAccessException {
    // sets current game. used by createGame while creating, public used while joining
    // checks for the bounds in the game: if not out of the participantsNumber, and only if game status is 0
    // to clear the current game, gameId = null should be set

    Integer userId = getUserId(session);
    if (userId == null) {
      throw new DatabaseAccessException(); // if there is no such user
    }

    if (gameId == null) {
      PreparedStatement updData = conn.prepareStatement("UPDATE users SET current_game_id = NULL WHERE session = ?");
      updData.setString(1, session);
      updData.executeUpdate();
    } else {
      PreparedStatement selParticipantsNumber = conn.prepareStatement("SELECT participants_number FROM games WHERE id = ? AND status = 0");
      selParticipantsNumber.setInt(1, gameId);
      ResultSet rsParticipantsNumber = selParticipantsNumber.executeQuery();
      if (rsParticipantsNumber.next()) {
        Integer participantsNumber = rsParticipantsNumber.getInt(1);
        if (Objects.equals(getCurrentParticipantsNumber(gameId), participantsNumber)) {
          throw new DatabaseAccessException("Request is out of bounds"); // the request is out of the maximum participants number bound
        } else {
          PreparedStatement updData = conn.prepareStatement("UPDATE users SET current_game_id = ? WHERE session = ?");
          updData.setInt(1, gameId);
          updData.setString(2, session);
          updData.executeUpdate();
        }
      } else {
        throw new DatabaseAccessException("Game not found"); // game not found / the game is not with 0 status (already started)
      }
    }
  }

  public Integer getCurrentGame(String session) throws SQLException, DatabaseAccessException {
    // gets current game
    
    PreparedStatement selCurrentGame = conn.prepareStatement("SELECT current_game_id FROM users WHERE session = ?");
    selCurrentGame.setString(1, session);
    ResultSet rsCurrentGame = selCurrentGame.executeQuery();
    if (rsCurrentGame.next()) {
      int res = rsCurrentGame.getInt(1);
      return res == 0 ? null : res; // gameId, null if is not in game
    } else {
      throw new DatabaseAccessException(); // if not exists
    }
  }

  public void addGamePlayed(String session) throws SQLException, DatabaseAccessException {
    // adds a new game played
    
    Integer gameId = getCurrentGame(session);
    Integer userId = getUserId(session);
    if (userId == null) {
      throw new DatabaseAccessException(); // if there is no such user
    }
    if (!(Objects.equals(gameId, null))) {
      PreparedStatement updData = conn.prepareStatement("UPDATE users SET games_played_number = games_played_number + 1 WHERE session = ?; INSERT INTO games_history (game_id, user_id) VALUES (?, ?);");
      updData.setString(1, session);
      updData.setInt(2, gameId);
      updData.setInt(3, userId);

      updData.executeUpdate();
    } else {
      throw new DatabaseAccessException("Game not found");
    }
  }

  public Integer[] getGamesPlayed(String session) throws SQLException, DatabaseAccessException {
    // collects all the data about user's game history
    
    Integer userId = getUserId(session);
    if (userId == null) {
      throw new DatabaseAccessException(); // if there is no such user
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

  public void addGlobalPoints(String session, int points) throws SQLException, DatabaseAccessException {
    // adds global points to the user's statistics data

    Integer userId = getUserId(session);
    if (userId == null) {
      throw new DatabaseAccessException(); // if there is no such user
    }
    PreparedStatement updData = conn.prepareStatement("UPDATE users SET global_points = global_points + ? WHERE session = ?");
    updData.setInt(1, points);
    updData.setString(2, session);
    updData.executeUpdate();
  }

  public Integer getGlobalPoints(String session) throws SQLException, DatabaseAccessException {
    // gets global points
    
    PreparedStatement selGlobalPoints = conn.prepareStatement("SELECT global_points FROM users WHERE session = ?");
    selGlobalPoints.setString(1, session);
    ResultSet rsGlobalPoints = selGlobalPoints.executeQuery();
    if (rsGlobalPoints.next()) {
      return rsGlobalPoints.getInt(1); // globalPoints
    } else {
      throw new DatabaseAccessException(); // if not exists
    }
  }

  public void addGlobalPossiblePoints(String session, int possiblePoints) throws SQLException, DatabaseAccessException {
    // adds global possible points to the user's statistics data

    Integer userId = getUserId(session);
    if (userId == null) {
      throw new DatabaseAccessException(); // if there is no such user
    }
    PreparedStatement updData = conn.prepareStatement("UPDATE users SET global_possible_points = global_possible_points + ? WHERE session = ?");
    updData.setInt(1, possiblePoints);
    updData.setString(2, session);
    updData.executeUpdate();
  }

  public Integer getGlobalPossiblePoints(String session) throws SQLException, DatabaseAccessException {
    // gets global possible points
    
    PreparedStatement selGlobalPossiblePoints = conn.prepareStatement("SELECT global_possible_points FROM users WHERE session = ?");
    selGlobalPossiblePoints.setString(1, session);
    ResultSet rsGlobalPossiblePoints = selGlobalPossiblePoints.executeQuery();
    if (rsGlobalPossiblePoints.next()) {
      return rsGlobalPossiblePoints.getInt(1); // globalPossiblePoints
    } else {
      throw new DatabaseAccessException(); // if not exists
    }
  }

  public void addCurrentGamePoints(String session, int points) throws SQLException, DatabaseAccessException {
    // adds current game points to the user's statistics data

    Integer userId = getUserId(session);
    if (userId == null) {
      throw new DatabaseAccessException(); // if there is no such user
    }
    PreparedStatement updData = conn.prepareStatement("UPDATE users SET current_game_points = current_game_points + ? WHERE session = ?");
    updData.setInt(1, points);
    updData.setString(2, session);
    updData.executeUpdate();
  }

  public Integer getCurrentGamePoints(String session) throws SQLException, DatabaseAccessException {
    // gets current game points
    
    PreparedStatement selCurrentGamePoints = conn.prepareStatement("SELECT current_game_points FROM users WHERE session = ?");
    selCurrentGamePoints.setString(1, session);
    ResultSet rsCurrentGamePoints = selCurrentGamePoints.executeQuery();
    if (rsCurrentGamePoints.next()) {
      return rsCurrentGamePoints.getInt(1); // currentGamePoints
    } else {
      throw new DatabaseAccessException(); // if not exists
    }
  }

  public void logOut(String session) throws SQLException, DatabaseAccessException {
    // erases the session info in the users data
    // adds global points to the user's statistics data

    Integer userId = getUserId(session);
    if (userId == null) {
      throw new DatabaseAccessException(); // if there is no such user
    }
    PreparedStatement updData = conn.prepareStatement("UPDATE users SET session = NULL WHERE session = ?");
    updData.setString(1, session);
    updData.executeUpdate();
  }

  //
  // Games TABLE REFERRED METHODS
  //

  public Integer createGame(String sessionOfAuthor, int levelDifficulty, int numberOfQuestions, int participantsNumber, int topicId) throws SQLException, DatabaseAccessException {
    // creates the game room, returns gameId
    // default isPrivate = true
    Integer authorId = getUserId(sessionOfAuthor);
    if (authorId == null) {
      throw new DatabaseAccessException(); // if author not exists
    }
    if (!(levelDifficulty >= 1 && levelDifficulty <= 3) || participantsNumber < 4) {
      throw new DatabaseAccessException("Bad params"); // if provided bad initialization params while using method
    }

    PreparedStatement inpGame = conn.prepareStatement("INSERT INTO games " +
      "(status, author_id, created_at, is_private, level_difficulty, current_question_number, number_of_questions, participants_number, topic_id) " +
      "VALUES (0, ?, ?, TRUE, ?, 0, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
    inpGame.setInt(1, authorId);
    inpGame.setTimestamp(2, Timestamp.from(Instant.now()));
    inpGame.setInt(3, levelDifficulty);
    inpGame.setInt(4, numberOfQuestions);
    inpGame.setInt(5, participantsNumber);
    inpGame.setInt(6, topicId);

    inpGame.executeUpdate();
    ResultSet rsInpGame = inpGame.getGeneratedKeys();
    if (rsInpGame.next()) {
      return rsInpGame.getInt(1);
    } else {
      return null;
    }
  }

  public Boolean checkGameExists(int gameId) throws SQLException {
    // checks if game exists or not
    
    PreparedStatement selExists = conn.prepareStatement("SELECT * FROM games WHERE id = ?");
    selExists.setInt(1, gameId);
    ResultSet rsSelExists = selExists.executeQuery();
    return rsSelExists.isBeforeFirst(); // true if game exists, false if not exists
  }

  public Integer[] getPreset(int gameId) throws SQLException, DatabaseAccessException {
    // gets the preset of the game (authorId, levelDifficulty, numberOfQuestions, participantsNumber, topicId)

    PreparedStatement selPreset = conn.prepareStatement("SELECT author_id, level_difficulty, number_of_questions, participants_number, topic_id FROM games WHERE id = ?");
    selPreset.setInt(1, gameId);
    ResultSet rsPreset = selPreset.executeQuery();
    if (rsPreset.next()) {
      Integer[] preset = new Integer[5];
      preset[0] = rsPreset.getInt("author_id");
      preset[1] = rsPreset.getInt("level_difficulty");
      preset[2] = rsPreset.getInt("number_of_questions");
      preset[3] = rsPreset.getInt("participants_number");
      preset[4] = rsPreset.getInt("topic_id");
      return preset;
    } else {
      throw new DatabaseAccessException(); // if not exists
    }
  }

  public void setStatus(int gameId, int status) throws SQLException, DatabaseAccessException {
    // sets status for the game
    // 0 - pending, 1 - paused, 2 - active, 3 - ended

    if (!checkGameExists(gameId)) {
      throw new DatabaseAccessException(); // game not found
    }
    PreparedStatement updData = conn.prepareStatement("UPDATE games SET status = ? WHERE id = ?");
    updData.setInt(1, status);
    updData.setInt(2, gameId);
    updData.executeUpdate();
  }

  public Integer getStatus(int gameId) throws SQLException, DatabaseAccessException {
    // gets status for the game
    
    PreparedStatement selStatus = conn.prepareStatement("SELECT status FROM games WHERE id = ?");
    selStatus.setInt(1, gameId);
    ResultSet rsStatus = selStatus.executeQuery();
    if (rsStatus.next()) {
      return rsStatus.getInt(1); // status: 0 - pending, 1 - paused, 2 - active, 3 - ended
    } else {
      throw new DatabaseAccessException(); // if not exists
    }
  }

  public void setGameStartTime(int gameId, Instant gameStartTime) throws SQLException, DatabaseAccessException {
    // sets the time of start of the game

    if (!checkGameExists(gameId)) {
      throw new DatabaseAccessException(); // game not found
    }
    PreparedStatement updData = conn.prepareStatement("UPDATE games SET game_start_time = ? WHERE id = ?");
    updData.setTimestamp(1, Timestamp.from(gameStartTime));
    updData.setInt(2, gameId);
    updData.executeUpdate();
  }

  public Timestamp getGameStartTime(int gameId) throws SQLException, DatabaseAccessException {
    // gets the time of start of the game
    
    PreparedStatement selTime = conn.prepareStatement("SELECT game_start_time FROM games WHERE id = ?");
    selTime.setInt(1, gameId);
    ResultSet rsTime = selTime.executeQuery();
    if (rsTime.next()) {
      return rsTime.getTimestamp(1); // timestamp
    } else {
      throw new DatabaseAccessException(); // if not exists
    }
  }

  public void setPrivate(int gameId, boolean isPrivate) throws SQLException, DatabaseAccessException {
    // sets the privateness option for the game

    if (!checkGameExists(gameId)) {
      throw new DatabaseAccessException(); // game not found
    }
    PreparedStatement updData = conn.prepareStatement("UPDATE games SET is_private = ? WHERE id = ?");
    updData.setBoolean(1, isPrivate);
    updData.setInt(2, gameId);
    updData.executeUpdate();
  }

  public Boolean getPrivate(int gameId) throws SQLException, DatabaseAccessException {
    // gets the privateness option of the game
    
    PreparedStatement selPrivate = conn.prepareStatement("SELECT is_private FROM games WHERE id = ?");
    selPrivate.setInt(1, gameId);
    ResultSet rsTime = selPrivate.executeQuery();
    if (rsTime.next()) {
      return rsTime.getBoolean(1); // true - is private, false - is not
    } else {
      throw new DatabaseAccessException(); // if not exists
    }
  }

  public void setGameEndTime(int gameId, Instant gameEndTime) throws SQLException, DatabaseAccessException {
    // sets the time of ending for the game

    if (!checkGameExists(gameId)) {
      throw new DatabaseAccessException(); // game not found
    }
    PreparedStatement updData = conn.prepareStatement("UPDATE games SET game_end_time = ? WHERE id = ?");
    updData.setTimestamp(1, Timestamp.from(gameEndTime));
    updData.setInt(2, gameId);
    updData.executeUpdate();
  }

  public Timestamp getGameEndTime(int gameId) throws SQLException, DatabaseAccessException {
    // gets time of end of the game
    
    PreparedStatement selTime = conn.prepareStatement("SELECT game_end_time FROM games WHERE id = ?");
    selTime.setInt(1, gameId);
    ResultSet rsTime = selTime.executeQuery();
    if (rsTime.next()) {
      return rsTime.getTimestamp(1); // timestamp
    } else {
      throw new DatabaseAccessException(); // if not exists
    }
  }

  public void stopGame(int gameId) throws SQLException, DatabaseAccessException {
    // stops the game in Games table, must be used with addGamePlayed

    if (checkGameExists(gameId)) {
      setStatus(gameId, 3);
      setGameEndTime(gameId, Instant.now());

      PreparedStatement updData = conn.prepareStatement("UPDATE users SET current_game_id = NULL WHERE current_game_id = ?");
      updData.setInt(1, gameId);
      updData.executeUpdate();
    } else {
      throw new DatabaseAccessException(); // if not exists
    }
  }

  public void deleteGame(int gameId) throws SQLException, DatabaseAccessException {
    // deletes the game

    if (checkGameExists(gameId)) {
      PreparedStatement updData = conn.prepareStatement("UPDATE users SET current_game_id = NULL WHERE current_game_id = ?; " +
          "DELETE FROM games WHERE id = ?");
      updData.setInt(1, gameId);
      updData.setInt(2, gameId);
      updData.executeUpdate();
    } else {
      throw new DatabaseAccessException(); // if not exists
    }
  }

  public Integer getCurrentQuestionNumber(int gameId) throws SQLException, DatabaseAccessException {
    // gets the current question number of the game
    
    PreparedStatement selCurrQuestion = conn.prepareStatement("SELECT current_question_number FROM games WHERE id = ?");
    selCurrQuestion.setInt(1, gameId);
    ResultSet rsCurrQuestion = selCurrQuestion.executeQuery();
    if (rsCurrQuestion.next()) {
      return rsCurrQuestion.getInt(1); // currentQuestionNumber
    } else {
      throw new DatabaseAccessException(); // if not exists
    }
  }

  public Question nextQuestion(int gameId) throws SQLException, DatabaseAccessException {
    // returns a new object of next question
    if (!checkGameExists(gameId)) {
      throw new DatabaseAccessException(); // game not found
    }

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
      throw new DatabaseAccessException(); // game not found
    }

    PreparedStatement selQuestion = conn.prepareStatement("SELECT question_text, answer1, answer2, answer3, answer4" +
      " FROM questions WHERE question_number = ? AND game_id = ?");
    selQuestion.setInt(1, currentNum);
    selQuestion.setInt(2, gameId);

    ResultSet rsQuestion = selQuestion.executeQuery();
    if (rsQuestion.next()) {
      Question questionObj = new Question();
      questionObj.setQuestionText(rsQuestion.getString(1));
      questionObj.setAnswer1(rsQuestion.getString(2));
      questionObj.setAnswer2(rsQuestion.getString(3));
      questionObj.setAnswer3(rsQuestion.getString(4));
      questionObj.setAnswer4(rsQuestion.getString(5));

      return questionObj; // Question
    } else {
      throw new DatabaseAccessException("Next question not exists"); // questions not found
    }
  }

  public Integer getRightAnswer(int gameId, int questionNumber) throws SQLException, DatabaseAccessException {
    // gets right answer for a question number

    if (!checkGameExists(gameId)) {
      throw new DatabaseAccessException(); // game not found
    }
    Integer gameStatus = getStatus(gameId);
    if (gameStatus != 2) {
      throw new DatabaseAccessException("Game is not active"); // if game is not active
    }
    PreparedStatement selRight = conn.prepareStatement("SELECT right_answer_number FROM questions WHERE question_number = ? AND game_id = ?");
    selRight.setInt(1, questionNumber);
    selRight.setInt(2, gameId);
    ResultSet rsRight = selRight.executeQuery();
    if (rsRight.next()) {
      return rsRight.getInt(1);
    } else {
      throw new DatabaseAccessException();
    }
  }


  public void loadQuestions(int gameId, JSONArray jsonArr) throws SQLException, DatabaseAccessException {
    // example of json argument is described in the documentation catalog
    // loads questions data from json and commits it into the database

    if (!checkGameExists(gameId)) {
      throw new DatabaseAccessException(); // game not found
    }

    final int ANSWER_AMOUNT = 4;

    for (int i = 0; i < jsonArr.length(); i++) {
      JSONObject itemObject = jsonArr.getJSONObject(i);
      int questionNumber = itemObject.getInt("question_number");
      String questionText = itemObject.getString("question_text");
      int rightAnswerNumber = itemObject.getInt("right_answer_number");
      PreparedStatement inpQuestion = conn.prepareStatement("INSERT INTO questions" +
        " (game_id, question_number, question_text, right_answer_number, answer1, answer2, answer3, answer4)" +
        " VALUES (?, ?, ?, ?, ?, ?, ?, ?)");
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

  public JSONArray getGameLeaderboards(int gameId) throws SQLException, DatabaseAccessException {
    // gets leaderboards of current game as JSONArray
    // return [[username, currentGamePoints], ...] // example : [["ultra_evgeniy1337", 1500], ["alexander_under", 1250], ...]

    if (!checkGameExists(gameId)) {
      throw new DatabaseAccessException(); // game not found
    }

    PreparedStatement selUsers = conn.prepareStatement("SELECT username, current_game_points FROM users " +
        "WHERE current_game_id = ? ORDER BY current_game_points DESC");
    selUsers.setInt(1, gameId);
    ResultSet rsUsers = selUsers.executeQuery();
    JSONArray res = new JSONArray();
    while (rsUsers.next()) {
      res.put(new JSONArray().put(rsUsers.getString("username")).put(rsUsers.getInt("current_game_points")));
    }

    return res;
  }

  public JSONArray getGlobalLeaderboards() throws SQLException {
    // gets global leaderboards TOP-100 as JSONArray
    // return [[userId, username, globalPoints, globalPossiblePoints], ...]
    // example : [[571, "ultra_evgeniy1337", 150014, 1234567], [1693, "alexander_under", 125017, 2345678], ...]

    Statement selUsers = conn.createStatement();
    ResultSet rsUsers = selUsers.executeQuery("SELECT id, username, global_points, global_possible_points " +
        "FROM users ORDER BY global_points DESC, global_possible_points ASC LIMIT 100");
    JSONArray res = new JSONArray();
    while (rsUsers.next()) {
      JSONArray putObj = new JSONArray();
      putObj.put(rsUsers.getInt("id"));
      putObj.put(rsUsers.getString("username"));
      putObj.put(rsUsers.getInt("global_points"));
      putObj.put(rsUsers.getInt("global_possible_points"));
      res.put(putObj);
    }

    return res;
  }

  public Integer getCurrentParticipantsNumber(int gameId) throws SQLException, DatabaseAccessException {
    // gets current participants number by gameId

    if (!checkGameExists(gameId)) {
      throw new DatabaseAccessException(); // game not found
    }
    PreparedStatement selUsers = conn.prepareStatement("SELECT COUNT(id) FROM users WHERE current_game_id = ?");
    selUsers.setInt(1, gameId);
    ResultSet rsUsers = selUsers.executeQuery();
    if (rsUsers.next()) {
      return rsUsers.getInt(1);
    } else {
      throw new DatabaseAccessException();
    }
  }

  public JSONArray getOpenGames() throws SQLException, DatabaseAccessException {
    // gets currently open games as JSONArray, sorted by id ascending
    // return [[gameId, topicId, currentParticipantsNumber, participantsNumber], ...]
    // example : [[25, 2, 4, 5], [36, 3, 4, 6], ...]

    Statement selGames = conn.createStatement();
    ResultSet rsGames = selGames.executeQuery("SELECT id, topic_id, participants_number " +
        "FROM games WHERE is_private = FALSE AND status = 0");
    JSONArray res = new JSONArray();
    while (rsGames.next()) {
      JSONArray putObj = new JSONArray();
      int gameId = rsGames.getInt("id");
      putObj.put(gameId);
      putObj.put(rsGames.getInt("topic_id"));
      putObj.put(getCurrentParticipantsNumber(gameId));
      putObj.put(rsGames.getInt("participants_number"));
      res.put(putObj);
    }

    return res;
  }

  //
  // Achievements TABLE REFERRED METHODS
  //

  public Integer addAchievement(Achievement achievement) throws SQLException {
    // creates a new achievement, returns id of it
    
    PreparedStatement inpAchvm = conn.prepareStatement("INSERT INTO achievements (name, profile_pic_needed, description_needed, " +
        "games_number_needed, global_points_needed, global_rating_place_needed, current_game_points_needed, current_game_rating_needed," +
        "current_game_level_difficulty_needed) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
    inpAchvm.setString(1, achievement.getName());
    inpAchvm.setBoolean(2, achievement.isProfilePicNeeded());
    inpAchvm.setBoolean(3, achievement.isDescriptionNeeded());
    inpAchvm.setInt(4, achievement.getGamesNumberNeeded());
    inpAchvm.setInt(5, achievement.getGlobalPointsNeeded());
    inpAchvm.setInt(6, achievement.getGlobalRatingPlaceNeeded());
    inpAchvm.setInt(7, achievement.getCurrentGamePointsNeeded());
    inpAchvm.setInt(8, achievement.getCurrentGameRatingNeeded());
    inpAchvm.setInt(9, achievement.getCurrentGameLevelDifficultyNeeded());
    
    inpAchvm.executeUpdate();
    ResultSet rsInpAchvm = inpAchvm.getGeneratedKeys();
    if (rsInpAchvm.next()) {
      return rsInpAchvm.getInt(1);
    } else {
      return null;
    }
  }
  
  public void attachAchievement(String session, int achievementId) throws SQLException, DatabaseAccessException {
    // attaches achievement to user
    
    Integer userId = getUserId(session);
    if (userId != null) {
      PreparedStatement inpAchvm = conn.prepareStatement("INSERT INTO user_achievements (user_id, achievement_id) VALUES (?, ?)");
      inpAchvm.setInt(1, userId);
      inpAchvm.setInt(2, achievementId);
      
      inpAchvm.executeUpdate();
    } else {
      throw new DatabaseAccessException();
    }
  }

  public Integer[] checkAchievementAchieved(String session, Achievement achieved) throws SQLException, DatabaseAccessException {
    // checks if achievement achieved, params of actions user achieved need to be specified in (Achievement) fields
    // returns id list of achievements
    
    Integer userId = getUserId(session);
    if (userId == null) {
      throw new DatabaseAccessException(); // if not exists
    }
    ArrayList<Integer> res = new ArrayList<>();

    String sql = "SELECT a.id FROM achievements a " +
      "LEFT JOIN user_achievements ua ON a.id = ua.achievement_id AND ua.user_id = ? " +
      "WHERE a.profile_pic_needed = ? " +
      "AND a.description_needed = ?" +
      "AND a.games_number_needed >= ? AND a.global_points_needed >= ? " +
      "AND a.global_rating_place_needed >= ? AND a.current_game_points_needed >= ? " +
      "AND a.current_game_rating_needed >= ? AND a.current_game_level_difficulty_needed >= ? " +
      "AND ua.achievement_id IS NULL";
    PreparedStatement selAchievement = conn.prepareStatement(sql);

    selAchievement.setInt(1, userId);
    selAchievement.setBoolean(2, achieved.isProfilePicNeeded());
    selAchievement.setBoolean(3, achieved.isDescriptionNeeded());
    selAchievement.setInt(4, achieved.getGamesNumberNeeded());
    selAchievement.setInt(5, achieved.getGlobalPointsNeeded());
    selAchievement.setInt(6, achieved.getGlobalRatingPlaceNeeded());
    selAchievement.setInt(7, achieved.getCurrentGamePointsNeeded());
    selAchievement.setInt(8, achieved.getCurrentGameRatingNeeded());
    selAchievement.setInt(9, achieved.getCurrentGameLevelDifficultyNeeded());

    ResultSet rsAchievement = selAchievement.executeQuery();
    while (rsAchievement.next()) {
      res.add(rsAchievement.getInt("id"));
    }

    return res.toArray(new Integer[0]);
  }

  public Integer[] getAchievementsOf(String session) throws SQLException, DatabaseAccessException {
    // returns id list of achievements user already have

    Integer userId = getUserId(session);
    if (userId == null) {
      throw new DatabaseAccessException(); // if not exists
    }
    ArrayList<Integer> res = new ArrayList<>();
    PreparedStatement selAchievement = conn.prepareStatement("SELECT achievement_id FROM user_achievements WHERE user_id = ? ORDER BY achievement_id ASC");
    selAchievement.setInt(1, userId);
    ResultSet rsAchievement = selAchievement.executeQuery();
    while (rsAchievement.next()) {
      res.add(rsAchievement.getInt("achievement_id"));
    }

    return res.toArray(new Integer[0]);
  }

  public Achievement getAchievementById(int achievementId) throws SQLException, DatabaseAccessException {
    // returns achievement object by id

    PreparedStatement selAchievement = conn.prepareStatement("SELECT * FROM achievements WHERE id = ?");
    selAchievement.setInt(1, achievementId);
    ResultSet rsAchievement = selAchievement.executeQuery();
    Achievement achievementObj = new Achievement();

    if (rsAchievement.next()) {
      achievementObj.setAchievementId(rsAchievement.getInt("id"));
      achievementObj.setName(rsAchievement.getString("name"));
      achievementObj.setProfilePicNeeded(rsAchievement.getBoolean("profile_pic_needed"));
      achievementObj.setDescriptionNeeded(rsAchievement.getBoolean("description_needed"));
      achievementObj.setGamesNumberNeeded(rsAchievement.getInt("games_number_needed"));
      achievementObj.setGlobalPointsNeeded(rsAchievement.getInt("global_points_needed"));
      achievementObj.setGlobalRatingPlaceNeeded(rsAchievement.getInt("global_rating_place_needed"));
      achievementObj.setCurrentGamePointsNeeded(rsAchievement.getInt("current_game_points_needed"));
      achievementObj.setCurrentGameRatingNeeded(rsAchievement.getInt("current_game_rating_needed"));
      achievementObj.setCurrentGameLevelDifficultyNeeded(rsAchievement.getInt("current_game_level_difficulty_needed"));

      return achievementObj;
    } else {
      throw new DatabaseAccessException(); // not found
    }
  }

  public Achievement[] getAllAchievements() throws SQLException {
    // returns achievement list
    
    ArrayList<Achievement> res = new ArrayList<>();
    Statement selAchievement = conn.createStatement();
    ResultSet rsAchievement = selAchievement.executeQuery("SELECT * FROM achievements ORDER BY id ASC");

    while (rsAchievement.next()) {
      Achievement achievementObj = new Achievement();

      achievementObj.setAchievementId(rsAchievement.getInt("id"));
      achievementObj.setName(rsAchievement.getString("name"));
      achievementObj.setProfilePicNeeded(rsAchievement.getBoolean("profile_pic_needed"));
      achievementObj.setDescriptionNeeded(rsAchievement.getBoolean("description_needed"));
      achievementObj.setGamesNumberNeeded(rsAchievement.getInt("games_number_needed"));
      achievementObj.setGlobalPointsNeeded(rsAchievement.getInt("global_points_needed"));
      achievementObj.setGlobalRatingPlaceNeeded(rsAchievement.getInt("global_rating_place_needed"));
      achievementObj.setCurrentGamePointsNeeded(rsAchievement.getInt("current_game_points_needed"));
      achievementObj.setCurrentGameRatingNeeded(rsAchievement.getInt("current_game_rating_needed"));
      achievementObj.setCurrentGameLevelDifficultyNeeded(rsAchievement.getInt("current_game_level_difficulty_needed"));

      res.add(achievementObj);
    }

    return res.toArray(new Achievement[0]);
  }

  public void removeAchievement(int achievementId) throws SQLException {
    // removes an existing achievement
    
    PreparedStatement delAchievement = conn.prepareStatement("DELETE FROM achievements WHERE id = ?");
    delAchievement.setInt(1, achievementId);
    delAchievement.executeUpdate();
  }

  //
  // Topics TABLE REFERRED METHODS
  //

  public Integer addTopic(Topic topic) throws SQLException {
    // creates a new topic, returns id of it

    PreparedStatement inpTopic = conn.prepareStatement("INSERT INTO topics (name) VALUES (?)", Statement.RETURN_GENERATED_KEYS);
    inpTopic.setString(1, topic.getName());

    inpTopic.executeUpdate();
    ResultSet rsInpTopic = inpTopic.getGeneratedKeys();
    if (rsInpTopic.next()) {
      return rsInpTopic.getInt(1);
    } else {
      return null;
    }
  }

  public Topic getTopicById(int id) throws SQLException, DatabaseAccessException {
    // returns topic object by id
    
    Topic topic = new Topic();
    PreparedStatement inpTopic = conn.prepareStatement("SELECT name FROM topics WHERE id = ?");
    inpTopic.setInt(1, id);
    
    ResultSet rsInpTopic = inpTopic.executeQuery();
    if (rsInpTopic.next()) {
      topic.setTopicId(id);
      topic.setName(rsInpTopic.getString(1));
      return topic;
    } else {
      throw new DatabaseAccessException();
    }
  }

  public Topic[] getAllTopics() throws SQLException {
    // returns topic list

    ArrayList<Topic> res = new ArrayList<>();
    Statement selTopic = conn.createStatement();
    ResultSet rsTopic = selTopic.executeQuery("SELECT * FROM topics");

    while (rsTopic.next()) {
      Topic topicObj = new Topic();
      topicObj.setTopicId(rsTopic.getInt("id"));
      topicObj.setName(rsTopic.getString("name"));
      res.add(topicObj);
    }
    
    return res.toArray(new Topic[0]);
  }

  public void removeTopic(int topicId) throws SQLException {
    // removes an existing topic

    PreparedStatement delAchievement = conn.prepareStatement("DELETE FROM topics WHERE id = ?");
    delAchievement.setInt(1, topicId);
    delAchievement.executeUpdate();
  }

  //
  // AUXILIARY METHODS FOR TESTING
  //

  public void eraseAllData(String keyword) throws SQLException {
    if (keyword.equals("DELETE_ALL_RECORDS_IN_DATABASE")) {
      Statement delData = conn.createStatement();
      delData.executeUpdate("DELETE FROM users; DELETE FROM games; DELETE FROM achievements; DELETE FROM questions; " +
          "DELETE FROM games_history; DELETE FROM user_achievements; INSERT INTO games (status, participants_number) VALUES (0, 1);");
    }
  }
}
