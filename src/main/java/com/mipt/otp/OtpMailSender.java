package com.mipt.otp;

import java.time.Instant;

public interface OtpMailSender {
  void sendOtp(String email, OtpPurpose purpose, String code, Instant expiresAt);
}
