package com.mipt.otp;

import java.time.Duration;
import java.util.Optional;

public interface OtpStore {
  void saveOtp(String email, OtpPurpose purpose, String otpHash, Duration ttl);

  Optional<String> getOtpHash(String email, OtpPurpose purpose);

  void clearOtp(String email, OtpPurpose purpose);

  void resetAttempts(String email, OtpPurpose purpose, Duration ttl);

  int incrementAttempts(String email, OtpPurpose purpose);

  boolean hasCooldown(String email, OtpPurpose purpose);

  void saveCooldown(String email, OtpPurpose purpose, Duration ttl);

  long getCooldownSeconds(String email, OtpPurpose purpose);
}
