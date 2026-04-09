package com.mipt.otp;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum OtpPurpose {
  REGISTRATION("registration"),
  PASSWORD_RESET("password_reset");

  private final String value;

  OtpPurpose(String value) {
    this.value = value;
  }

  @JsonValue
  public String getValue() {
    return value;
  }

  @JsonCreator
  public static OtpPurpose fromValue(String value) {
    if (value == null) {
      return null;
    }

    return Arrays.stream(values())
        .filter(item -> item.value.equalsIgnoreCase(value) || item.name().equalsIgnoreCase(value))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Unsupported OTP purpose: " + value));
  }
}
