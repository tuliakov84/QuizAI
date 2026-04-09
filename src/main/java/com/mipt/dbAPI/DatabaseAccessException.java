package com.mipt.dbAPI;

public class DatabaseAccessException extends Exception {
  public DatabaseAccessException() {
    super("Not found");
  }

  public DatabaseAccessException(String message) {
    super(message);
  }

  public DatabaseAccessException(String message, Throwable cause) {
    super(message, cause);
  }
}
