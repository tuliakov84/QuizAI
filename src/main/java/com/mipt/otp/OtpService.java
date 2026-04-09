package com.mipt.otp;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

@Service
public class OtpService {
  private static final int NUMERIC_ALPHABET_SIZE = 10;

  private final OtpStore otpStore;
  private final OtpContextStore otpContextStore;
  private final OtpMailSender otpMailSender;
  private final OtpProperties properties;
  private final Clock clock;
  private final SecureRandom secureRandom;
  private final BCryptPasswordEncoder passwordEncoder;

  @Autowired
  public OtpService(
      OtpStore otpStore,
      OtpContextStore otpContextStore,
      OtpMailSender otpMailSender,
      OtpProperties properties
  ) {
    this(otpStore, otpContextStore, otpMailSender, properties, Clock.systemUTC(), new SecureRandom());
  }

  OtpService(
      OtpStore otpStore,
      OtpContextStore otpContextStore,
      OtpMailSender otpMailSender,
      OtpProperties properties,
      Clock clock,
      SecureRandom secureRandom
  ) {
    this.otpStore = otpStore;
    this.otpContextStore = otpContextStore;
    this.otpMailSender = otpMailSender;
    this.properties = properties;
    this.clock = clock;
    this.secureRandom = secureRandom;
    this.passwordEncoder = new BCryptPasswordEncoder();
  }

  public OtpResponse sendOtp(OtpSendRequest request) {
    String email = normalizeEmail(request.email());
    OtpPurpose purpose = request.purpose();
    ensureContextExists(email, purpose);

    if (otpStore.hasCooldown(email, purpose)) {
      long retryAfterSeconds = otpStore.getCooldownSeconds(email, purpose);
      throw new OtpException(
          HttpStatus.TOO_MANY_REQUESTS,
          "OTP was already sent recently. Please wait before requesting another code.",
          retryAfterSeconds
      );
    }

    Duration ttl = Duration.ofSeconds(properties.getTtlSeconds());
    String code = generateCode();
    String otpHash = passwordEncoder.encode(code);
    Instant expiresAt = Instant.now(clock).plus(ttl);

    otpStore.saveOtp(email, purpose, otpHash, ttl);
    otpStore.resetAttempts(email, purpose, ttl);

    try {
      otpMailSender.sendOtp(email, purpose, code, expiresAt);
      otpStore.saveCooldown(email, purpose, Duration.ofSeconds(properties.getResendCooldownSeconds()));
      return OtpResponse.sent(email, purpose, expiresAt);
    } catch (RuntimeException exception) {
      otpStore.clearOtp(email, purpose);
      throw exception;
    }
  }

  public OtpResponse verifyOtp(OtpVerifyRequest request) {
    String email = normalizeEmail(request.email());
    OtpPurpose purpose = request.purpose();
    String code = request.code().trim();
    ensureContextExists(email, purpose);

    String otpHash = otpStore.getOtpHash(email, purpose)
        .orElseThrow(() -> new OtpException(HttpStatus.BAD_REQUEST, "OTP code is missing or expired"));

    int attempts = otpStore.incrementAttempts(email, purpose);
    if (attempts > properties.getMaxVerificationAttempts()) {
      otpStore.clearOtp(email, purpose);
      throw new OtpException(HttpStatus.TOO_MANY_REQUESTS, "Too many invalid attempts. Request a new OTP code.");
    }

    if (!passwordEncoder.matches(code, otpHash)) {
      int attemptsLeft = Math.max(0, properties.getMaxVerificationAttempts() - attempts);
      throw new OtpException(
          HttpStatus.BAD_REQUEST,
          "OTP code is invalid. Remaining attempts: " + attemptsLeft
      );
    }

    otpStore.clearOtp(email, purpose);
    return OtpResponse.verified(email, purpose);
  }

  public OtpResponse sendStubOtp(OtpSendRequest request) {
    if (!properties.isTestEndpointEnabled()) {
      throw new OtpException(HttpStatus.FORBIDDEN, "Test OTP endpoint is disabled");
    }

    String email = normalizeEmail(request.email());
    OtpPurpose purpose = request.purpose();
    Instant expiresAt = Instant.now(clock).plus(Duration.ofSeconds(properties.getTtlSeconds()));

    otpMailSender.sendOtp(email, purpose, "000000", expiresAt);
    return OtpResponse.testSent(email, purpose, expiresAt);
  }

  private String normalizeEmail(String email) {
    return email.trim().toLowerCase(Locale.ROOT);
  }

  private void ensureContextExists(String email, OtpPurpose purpose) {
    if (otpContextStore.getContext(email, purpose).isEmpty()) {
      throw new OtpException(HttpStatus.BAD_REQUEST, "No active server-side request for this OTP operation");
    }
  }

  private String generateCode() {
    int codeLength = properties.getCodeLength();
    if (codeLength < 4 || codeLength > 8) {
      throw new OtpException(HttpStatus.INTERNAL_SERVER_ERROR, "OTP code length must be between 4 and 8 digits");
    }

    StringBuilder builder = new StringBuilder(codeLength);
    for (int i = 0; i < codeLength; i++) {
      builder.append(secureRandom.nextInt(NUMERIC_ALPHABET_SIZE));
    }
    return builder.toString();
  }
}
