package com.mipt.auth;

public record AuthActionResponse(
    String status,
    String message,
    String email
) {
  public static AuthActionResponse success(String status, String message, String email) {
    return new AuthActionResponse(status, message, email);
  }
}
