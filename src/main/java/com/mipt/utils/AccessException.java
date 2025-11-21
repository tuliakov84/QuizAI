package com.mipt.utils;

public class AccessException extends Exception {
  public AccessException() {
    super("Cannot access this resource");
  }

  public AccessException(String message) {
    super(message);
  }
}
