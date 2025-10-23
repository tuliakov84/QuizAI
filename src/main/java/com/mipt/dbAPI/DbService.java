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

  public class DBUser {
    private boolean checkUserExists(String username) throws SQLException {
      PreparedStatement selExists = conn.prepareStatement("SELECT * FROM users WHERE username = ?");
      selExists.setString(1, username);
      ResultSet rsSelExists = selExists.executeQuery();
      return rsSelExists != null; // true if user already exists, false if not exists
    }

    public boolean register(String username, String password) throws SQLException {
      // creates an account for a new user
      if (checkUserExists(username)) {
        return false;
      }
      String hashedPassword = BCrypt.withDefaults().hashToString(12, password.toCharArray());
      PreparedStatement inpData = conn.prepareStatement("INSERT INTO users (username, password) VALUES (?, ?)");
      inpData.setString(1, username);
      inpData.setString(2, hashedPassword);
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
        return true; // done
      } else {
        return false; // password is wrong
      }
    }

    public Achievement checkAchievement(String session) {
      return null; //
    }

    public void changePassword(String session, String newPassword) {
      //
    }

    public void changeProfilePic(String session, short picId) {
      //
    }

    public void changeDescription(String session, String description) {
      //
    }

    public void setLastActivity(String session, Instant time) {
      //
    }

    public void setCurrentGame(String session, Game game) {
      //
    }

    public void addGamePlayed(String session, Game game) {
      //
    }

    public void addGlobalPoints(String session, int points) {
      //
    }

    public void addGlobalPossiblePoints(String session, int possiblePoints) {
      //
    }

    public void addCurrentGamePoints(String session, int points) {
      //
    }

    public void logOut(String session) {
      //
    }
  }

  public class DBGame {
    public Integer createGame(Game game) {
      // needs author, topic, numberOfQuestions, levelDifficulty, participantNumber to initialize
      return null;
    }

    public void setStatus(int gameId, short status) {
      //
    }

    public void setCreatedAt(int gameId, Instant createdAt) {
      //
    }

    public void setGameStartTime(int gameId, Instant gameStartTime) {
      //
    }

    public void setGameEndTime(int gameId, Instant gameEndTime) {
      //
    }

    public Question nextQuestion(int gameId) {
      //
      return null;
    }

    public void setPrivate(int gameId, boolean isPrivate) {
      //
    }

    public void loadQuestions(JSONObject json) {
      // example of json argument is described in the documentation catalog
    }
  }

  public class DBAchievement {
    public void add(Achievement achievement) {
      //
    }

    public void remove(Achievement achievement) {
      //
    }
  }

  public class DBTopic {
    public void add(Topic topic) {
      //
    }

    public void remove(Topic topic) {
      //
    }
  }
}
