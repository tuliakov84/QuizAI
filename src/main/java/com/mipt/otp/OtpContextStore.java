package com.mipt.otp;

import java.time.Duration;
import java.util.Optional;

public interface OtpContextStore {
  void saveContext(String email, OtpPurpose purpose, String payload, Duration ttl);

  Optional<String> getContext(String email, OtpPurpose purpose);

  void clearContext(String email, OtpPurpose purpose);
}
