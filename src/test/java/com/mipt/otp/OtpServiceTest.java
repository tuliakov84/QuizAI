package com.mipt.otp;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class OtpServiceTest {

  @Test
  void shouldSendAndVerifyOtpOnce() {
    InMemoryOtpStore otpStore = new InMemoryOtpStore();
    InMemoryOtpContextStore otpContextStore = new InMemoryOtpContextStore();
    CapturingOtpMailSender mailSender = new CapturingOtpMailSender();
    otpContextStore.saveContext("user@example.com", OtpPurpose.REGISTRATION, "pending", Duration.ofMinutes(5));
    OtpService otpService = new OtpService(
        otpStore,
        otpContextStore,
        mailSender,
        properties(),
        Clock.fixed(Instant.parse("2026-03-24T10:00:00Z"), ZoneOffset.UTC),
        new SecureRandom()
    );

    OtpResponse sendResponse = otpService.sendOtp(new OtpSendRequest("USER@example.com", OtpPurpose.REGISTRATION));

    assertEquals("OTP_SENT", sendResponse.status());
    assertEquals("user@example.com", sendResponse.email());
    assertNotNull(mailSender.lastCode);

    OtpResponse verifyResponse = otpService.verifyOtp(
        new OtpVerifyRequest("user@example.com", OtpPurpose.REGISTRATION, mailSender.lastCode)
    );

    assertEquals("OTP_VERIFIED", verifyResponse.status());

    OtpException exception = assertThrows(
        OtpException.class,
        () -> otpService.verifyOtp(new OtpVerifyRequest("user@example.com", OtpPurpose.REGISTRATION, mailSender.lastCode))
    );
    assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
  }

  @Test
  void shouldBlockResendDuringCooldown() {
    InMemoryOtpStore otpStore = new InMemoryOtpStore();
    InMemoryOtpContextStore otpContextStore = new InMemoryOtpContextStore();
    CapturingOtpMailSender mailSender = new CapturingOtpMailSender();
    otpContextStore.saveContext("user@example.com", OtpPurpose.REGISTRATION, "pending", Duration.ofMinutes(5));
    OtpService otpService = new OtpService(
        otpStore,
        otpContextStore,
        mailSender,
        properties(),
        Clock.fixed(Instant.parse("2026-03-24T10:00:00Z"), ZoneOffset.UTC),
        new SecureRandom()
    );

    otpService.sendOtp(new OtpSendRequest("user@example.com", OtpPurpose.REGISTRATION));

    OtpException exception = assertThrows(
        OtpException.class,
        () -> otpService.sendOtp(new OtpSendRequest("user@example.com", OtpPurpose.REGISTRATION))
    );

    assertEquals(HttpStatus.TOO_MANY_REQUESTS, exception.getStatus());
    assertNotNull(exception.getRetryAfterSeconds());
  }

  @Test
  void shouldInvalidateOtpAfterTooManyWrongAttempts() {
    InMemoryOtpStore otpStore = new InMemoryOtpStore();
    InMemoryOtpContextStore otpContextStore = new InMemoryOtpContextStore();
    CapturingOtpMailSender mailSender = new CapturingOtpMailSender();
    otpContextStore.saveContext("user@example.com", OtpPurpose.PASSWORD_RESET, "pending", Duration.ofMinutes(5));
    OtpService otpService = new OtpService(
        otpStore,
        otpContextStore,
        mailSender,
        properties(),
        Clock.fixed(Instant.parse("2026-03-24T10:00:00Z"), ZoneOffset.UTC),
        new SecureRandom()
    );

    otpService.sendOtp(new OtpSendRequest("user@example.com", OtpPurpose.PASSWORD_RESET));

    for (int attempt = 1; attempt <= 5; attempt++) {
      OtpException exception = assertThrows(
          OtpException.class,
          () -> otpService.verifyOtp(new OtpVerifyRequest("user@example.com", OtpPurpose.PASSWORD_RESET, "000000"))
      );
      assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }

    OtpException exception = assertThrows(
        OtpException.class,
        () -> otpService.verifyOtp(new OtpVerifyRequest("user@example.com", OtpPurpose.PASSWORD_RESET, "000000"))
    );

    assertEquals(HttpStatus.TOO_MANY_REQUESTS, exception.getStatus());
    assertTrue(otpStore.getOtpHash("user@example.com", OtpPurpose.PASSWORD_RESET).isEmpty());
  }

  @Test
  void shouldCleanupOtpWhenMailSendingFails() {
    InMemoryOtpStore otpStore = new InMemoryOtpStore();
    InMemoryOtpContextStore otpContextStore = new InMemoryOtpContextStore();
    FailingOtpMailSender mailSender = new FailingOtpMailSender();
    otpContextStore.saveContext("user@example.com", OtpPurpose.REGISTRATION, "pending", Duration.ofMinutes(5));
    OtpService otpService = new OtpService(
        otpStore,
        otpContextStore,
        mailSender,
        properties(),
        Clock.fixed(Instant.parse("2026-03-24T10:00:00Z"), ZoneOffset.UTC),
        new SecureRandom()
    );

    assertThrows(
        OtpException.class,
        () -> otpService.sendOtp(new OtpSendRequest("user@example.com", OtpPurpose.REGISTRATION))
    );

    assertTrue(otpStore.getOtpHash("user@example.com", OtpPurpose.REGISTRATION).isEmpty());
  }

  @Test
  void shouldSendStubOtpWithoutSavingInStore() {
    InMemoryOtpStore otpStore = new InMemoryOtpStore();
    InMemoryOtpContextStore otpContextStore = new InMemoryOtpContextStore();
    CapturingOtpMailSender mailSender = new CapturingOtpMailSender();
    OtpService otpService = new OtpService(
        otpStore,
        otpContextStore,
        mailSender,
        properties(),
        Clock.fixed(Instant.parse("2026-03-24T10:00:00Z"), ZoneOffset.UTC),
        new SecureRandom()
    );

    OtpResponse response = otpService.sendStubOtp(new OtpSendRequest("USER@example.com", OtpPurpose.REGISTRATION));

    assertEquals("OTP_TEST_SENT", response.status());
    assertEquals("000000", mailSender.lastCode);
    assertTrue(otpStore.getOtpHash("user@example.com", OtpPurpose.REGISTRATION).isEmpty());
  }

  @Test
  void shouldRejectStubOtpWhenEndpointDisabled() {
    InMemoryOtpStore otpStore = new InMemoryOtpStore();
    InMemoryOtpContextStore otpContextStore = new InMemoryOtpContextStore();
    CapturingOtpMailSender mailSender = new CapturingOtpMailSender();
    OtpProperties properties = properties();
    properties.setTestEndpointEnabled(false);
    OtpService otpService = new OtpService(
        otpStore,
        otpContextStore,
        mailSender,
        properties,
        Clock.fixed(Instant.parse("2026-03-24T10:00:00Z"), ZoneOffset.UTC),
        new SecureRandom()
    );

    OtpException exception = assertThrows(
        OtpException.class,
        () -> otpService.sendStubOtp(new OtpSendRequest("user@example.com", OtpPurpose.REGISTRATION))
    );

    assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
  }

  @Test
  void shouldRejectOtpWithoutServerSideContext() {
    InMemoryOtpStore otpStore = new InMemoryOtpStore();
    InMemoryOtpContextStore otpContextStore = new InMemoryOtpContextStore();
    CapturingOtpMailSender mailSender = new CapturingOtpMailSender();
    OtpService otpService = new OtpService(
        otpStore,
        otpContextStore,
        mailSender,
        properties(),
        Clock.fixed(Instant.parse("2026-03-24T10:00:00Z"), ZoneOffset.UTC),
        new SecureRandom()
    );

    OtpException exception = assertThrows(
        OtpException.class,
        () -> otpService.sendOtp(new OtpSendRequest("user@example.com", OtpPurpose.REGISTRATION))
    );

    assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
  }

  private static OtpProperties properties() {
    OtpProperties properties = new OtpProperties();
    properties.setTtlSeconds(300);
    properties.setResendCooldownSeconds(60);
    properties.setMaxVerificationAttempts(5);
    properties.setCodeLength(6);
    properties.setTestEndpointEnabled(true);
    return properties;
  }

  private static class CapturingOtpMailSender implements OtpMailSender {
    private String lastCode;

    @Override
    public void sendOtp(String email, OtpPurpose purpose, String code, Instant expiresAt) {
      this.lastCode = code;
    }
  }

  private static class FailingOtpMailSender implements OtpMailSender {
    @Override
    public void sendOtp(String email, OtpPurpose purpose, String code, Instant expiresAt) {
      throw new OtpException(HttpStatus.INTERNAL_SERVER_ERROR, "SMTP error");
    }
  }

  private static class InMemoryOtpStore implements OtpStore {
    private final Map<String, ValueWithExpiry<String>> otpMap = new HashMap<>();
    private final Map<String, ValueWithExpiry<Integer>> attemptsMap = new HashMap<>();
    private final Map<String, ValueWithExpiry<String>> cooldownMap = new HashMap<>();

    @Override
    public void saveOtp(String email, OtpPurpose purpose, String otpHash, Duration ttl) {
      otpMap.put(key(email, purpose), new ValueWithExpiry<>(otpHash, ttl));
    }

    @Override
    public Optional<String> getOtpHash(String email, OtpPurpose purpose) {
      ValueWithExpiry<String> value = otpMap.get(key(email, purpose));
      if (value == null || value.isExpired()) {
        otpMap.remove(key(email, purpose));
        return Optional.empty();
      }
      return Optional.of(value.value());
    }

    @Override
    public void clearOtp(String email, OtpPurpose purpose) {
      otpMap.remove(key(email, purpose));
      attemptsMap.remove(key(email, purpose));
    }

    @Override
    public void resetAttempts(String email, OtpPurpose purpose, Duration ttl) {
      attemptsMap.put(key(email, purpose), new ValueWithExpiry<>(0, ttl));
    }

    @Override
    public int incrementAttempts(String email, OtpPurpose purpose) {
      ValueWithExpiry<Integer> current = attemptsMap.get(key(email, purpose));
      int nextValue = current == null || current.isExpired() ? 1 : current.value() + 1;
      Duration ttl = current == null ? Duration.ofMinutes(5) : current.ttl();
      attemptsMap.put(key(email, purpose), new ValueWithExpiry<>(nextValue, ttl));
      return nextValue;
    }

    @Override
    public boolean hasCooldown(String email, OtpPurpose purpose) {
      ValueWithExpiry<String> value = cooldownMap.get(key(email, purpose));
      if (value == null || value.isExpired()) {
        cooldownMap.remove(key(email, purpose));
        return false;
      }
      return true;
    }

    @Override
    public void saveCooldown(String email, OtpPurpose purpose, Duration ttl) {
      cooldownMap.put(key(email, purpose), new ValueWithExpiry<>("1", ttl));
    }

    @Override
    public long getCooldownSeconds(String email, OtpPurpose purpose) {
      ValueWithExpiry<String> value = cooldownMap.get(key(email, purpose));
      if (value == null || value.isExpired()) {
        cooldownMap.remove(key(email, purpose));
        return 0;
      }
      return value.ttl().toSeconds();
    }

    private String key(String email, OtpPurpose purpose) {
      return purpose.name() + ":" + email;
    }
  }

  private static class InMemoryOtpContextStore implements OtpContextStore {
    private final Map<String, String> contextMap = new HashMap<>();

    @Override
    public void saveContext(String email, OtpPurpose purpose, String payload, Duration ttl) {
      contextMap.put(key(email, purpose), payload);
    }

    @Override
    public Optional<String> getContext(String email, OtpPurpose purpose) {
      return Optional.ofNullable(contextMap.get(key(email, purpose)));
    }

    @Override
    public void clearContext(String email, OtpPurpose purpose) {
      contextMap.remove(key(email, purpose));
    }

    private String key(String email, OtpPurpose purpose) {
      return purpose.name() + ":" + email;
    }
  }

  private record ValueWithExpiry<T>(T value, Duration ttl, Instant createdAt) {
    private ValueWithExpiry(T value, Duration ttl) {
      this(value, ttl, Instant.now());
    }

    private boolean isExpired() {
      return Instant.now().isAfter(createdAt.plus(ttl));
    }
  }
}
