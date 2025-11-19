package com.mipt.utils;

public class AccessException extends Exception {
  public AccessException() {
    super("Not found");
  }

  public AccessException(String message) {
    super(message);
  }
}
