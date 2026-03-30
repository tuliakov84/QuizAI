package com.mipt.otp;

import java.time.Instant;

public record OtpResponse(
    String status,
    String message,
    String email,
    OtpPurpose purpose,
    Instant expiresAt,
    Long retryAfterSeconds
) {
  public static OtpResponse sent(String email, OtpPurpose purpose, Instant expiresAt) {
    return new OtpResponse(
        "OTP_SENT",
        "OTP code was sent to the email address",
        email,
        purpose,
        expiresAt,
        null
    );
  }

  public static OtpResponse verified(String email, OtpPurpose purpose) {
    return new OtpResponse(
        "OTP_VERIFIED",
        "OTP code is valid",
        email,
        purpose,
        null,
        null
    );
  }

  public static OtpResponse testSent(String email, OtpPurpose purpose, Instant expiresAt) {
    return new OtpResponse(
        "OTP_TEST_SENT",
        "Stub OTP email was sent with a fake code",
        email,
        purpose,
        expiresAt,
        null
    );
  }

  public static OtpResponse error(String email, OtpPurpose purpose, String message, Long retryAfterSeconds) {
    return new OtpResponse(
        "OTP_ERROR",
        message,
        email,
        purpose,
        null,
        retryAfterSeconds
    );
  }
}
