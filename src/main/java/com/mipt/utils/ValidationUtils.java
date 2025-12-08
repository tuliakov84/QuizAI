package com.mipt.utils;

import java.util.regex.Pattern;

public class ValidationUtils {
  // Username: 6-30 characters, starts with letter, followed by letters, digits, or underscores
  private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z][a-zA-Z0-9_]{3,29}$");
  
  // Password: at least 8 characters, contains uppercase, lowercase, digit, and special character
  private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$");
  
  // Description: 10-200 characters, alphanumeric, spaces, periods, commas, hyphens
  private static final Pattern DESCRIPTION_PATTERN = Pattern.compile("^[a-zA-Zа-яА-Я0-9.,\\s-]{1,200}$");

  /**
   * Validates username format.
   * Requirements: 6-30 characters, starts with a letter, followed by letters, digits, or underscores.
   * 
   * @param userName the username to validate
   * @return true if valid, false otherwise
   */
  public static boolean usernameValidation(String userName) {
    if (userName == null || userName.isEmpty()) {
      return false;
    }
    return USERNAME_PATTERN.matcher(userName).matches();
  }

  /**
   * Validates password strength.
   * Requirements:
   * - At least 8 characters long
   * - At least one uppercase letter
   * - At least one lowercase letter
   * - At least one digit
   * - At least one special character from @$!%*?&
   * 
   * @param password the password to validate
   * @return true if valid, false otherwise
   */
  public static boolean passwordValidation(String password) {
    if (password == null || password.isEmpty()) {
      return false;
    }
    return PASSWORD_PATTERN.matcher(password).matches();
  }

  /**
   * Validates description format.
   * Requirements: 10-200 characters, contains only alphanumeric characters, spaces, periods, commas, and hyphens.
   * 
   * @param description the description to validate
   * @return true if valid, false otherwise
   */
  public static boolean descriptionValidation(String description) {
    if (description == null || description.isEmpty()) {
      return false;
    }
    return DESCRIPTION_PATTERN.matcher(description).matches();
  }
}
