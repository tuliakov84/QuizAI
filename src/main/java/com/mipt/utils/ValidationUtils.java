package com.mipt.utils;

import java.util.regex.Pattern;

public class ValidationUtils {
  // Имя пользователя: 6–30 символов, начинается с буквы, далее буквы, цифры или подчёркивания
  private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z][a-zA-Z0-9_]{3,29}$");
  
  // Пароль: не менее 8 символов, заглавная, строчная буква, цифра и спецсимвол
  private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$");
  
  // Описание: 10–200 символов, буквы, цифры, пробелы, точки, запятые, дефисы
  private static final Pattern DESCRIPTION_PATTERN = Pattern.compile("^[a-zA-Zа-яА-Я0-9.,\\s-]{1,200}$");

  /**
   * Проверяет формат имени пользователя.
   * Требования: 6–30 символов, начинается с буквы, далее буквы, цифры или подчёркивания.
   * 
   * @param userName проверяемое имя пользователя
   * @return true, если валидно, иначе false
   */
  public static boolean usernameValidation(String userName) {
    if (userName == null || userName.isEmpty()) {
      return false;
    }
    return USERNAME_PATTERN.matcher(userName).matches();
  }

  /**
   * Проверяет надёжность пароля.
   * Требования:
   * - Не менее 8 символов
   * - Минимум одна заглавная буква
   * - Минимум одна строчная буква
   * - Минимум одна цифра
   * - Минимум один спецсимвол из набора @$!%*?&
   * 
   * @param password проверяемый пароль
   * @return true, если валидно, иначе false
   */
  public static boolean passwordValidation(String password) {
    if (password == null || password.isEmpty()) {
      return false;
    }
    return PASSWORD_PATTERN.matcher(password).matches();
  }

  /**
   * Проверяет формат описания.
   * Требования: 10–200 символов, только буквы, цифры, пробелы, точки, запятые и дефисы.
   * 
   * @param description проверяемое описание
   * @return true, если валидно, иначе false
   */
  public static boolean descriptionValidation(String description) {
    if (description == null || description.isEmpty()) {
      return false;
    }
    return DESCRIPTION_PATTERN.matcher(description).matches();
  }
}
