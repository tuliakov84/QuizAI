package com.mipt.utils;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class ValidationUtils {
  private static final String USERNAME_REGEX = "^[a-zA-Z][a-zA-Z0-9_]{5,29}$";
  private static final String PASSWORD_REGEX = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\\\d)(?=.*[@$!%*?&])[A-Za-z\\\\d@$!%*?&]{8,}$";
  private static final String DESCRIPTION_REGEX = "^[a-zA-Z0-9.,\\\\s-]{10,200}$";

  public boolean usernameValidation(String userName) {
    // validates the username
    Pattern pattern = Pattern.compile(USERNAME_REGEX);
    Matcher matcher = pattern.matcher(userName);
    return matcher.matches();
  }

  public boolean passwordValidation(String password) {
    // validates the password
    // Regex pattern for a strong password:
    // - At least 8 characters long
    // - At least one uppercase letter
    // - At least one lowercase letter
    // - At least one digit
    // - At least one special character from @$!%*?&

    Pattern pattern = Pattern.compile(PASSWORD_REGEX);
    Matcher matcher = pattern.matcher(password);
    return matcher.matches();
  }

  public boolean descriptionValidation(String description) {
    // validates description
    // Regex for a bio: alphanumeric, spaces, periods, commas, hyphens between 10 and 200 characters long.

    Pattern pattern = Pattern.compile(DESCRIPTION_REGEX);
    Matcher matcher = pattern.matcher(description);
    return matcher.matches();
  }
}
