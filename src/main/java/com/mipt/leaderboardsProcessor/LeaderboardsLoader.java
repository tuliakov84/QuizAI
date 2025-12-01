package com.mipt.leaderboardsProcessor;

import org.json.JSONArray;

import java.sql.*;

public class LeaderboardsLoader {
  private final Connection conn;

  public LeaderboardsLoader(String url, String user, String password) {
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

  public JSONArray getGlobalLeaderboards() throws SQLException {
    // gets global leaderboards TOP-100 as JSONArray
    // return [[userId, username, globalPoints, globalPossiblePoints], ...]
    // example : [[571, "ultra_evgeniy1337", 150014, 1234567], [1693, "alexander_under", 125017, 2345678], ...]

    Statement selUsers = conn.createStatement();
    ResultSet rsUsers = selUsers.executeQuery("SELECT id, username, global_points, global_possible_points " +
      "FROM users ORDER BY global_points DESC, global_possible_points LIMIT 100");
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
}
