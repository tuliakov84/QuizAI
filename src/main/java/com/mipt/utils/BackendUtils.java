package com.mipt.utils;

import com.mipt.dbAPI.DatabaseAccessException;
import com.mipt.dbAPI.DbService;

import com.mipt.domainModel.Game;

import java.sql.SQLException;
import java.time.Instant;
import java.util.List;

public class BackendUtils {

  private static final int MAX_SESSION_RETRIES = 5;

  private final DbService dbService;

  public BackendUtils(DbService dbService) {
    this.dbService = dbService;
  }

  public boolean isSessionCollision(DatabaseAccessException exception) {
    String message = exception.getMessage();
    return message != null && message.contains("Session key is not unique");
  }

  public String generateSessionId() {
    return java.util.UUID.randomUUID().toString().replace("-", "");
  }
}
