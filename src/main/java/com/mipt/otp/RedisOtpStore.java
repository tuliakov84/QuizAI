package com.mipt.otp;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Component
public class RedisOtpStore implements OtpStore {
  private final StringRedisTemplate redisTemplate;

  public RedisOtpStore(StringRedisTemplate redisTemplate) {
    this.redisTemplate = redisTemplate;
  }

  @Override
  public void saveOtp(String email, OtpPurpose purpose, String otpHash, Duration ttl) {
    redisTemplate.opsForValue().set(codeKey(email, purpose), otpHash, ttl);
  }

  @Override
  public Optional<String> getOtpHash(String email, OtpPurpose purpose) {
    return Optional.ofNullable(redisTemplate.opsForValue().get(codeKey(email, purpose)));
  }

  @Override
  public void clearOtp(String email, OtpPurpose purpose) {
    redisTemplate.delete(codeKey(email, purpose));
    redisTemplate.delete(attemptKey(email, purpose));
  }

  @Override
  public void resetAttempts(String email, OtpPurpose purpose, Duration ttl) {
    redisTemplate.opsForValue().set(attemptKey(email, purpose), "0", ttl);
  }

  @Override
  public int incrementAttempts(String email, OtpPurpose purpose) {
    Long value = redisTemplate.opsForValue().increment(attemptKey(email, purpose));
    return value == null ? 0 : value.intValue();
  }

  @Override
  public boolean hasCooldown(String email, OtpPurpose purpose) {
    Boolean exists = redisTemplate.hasKey(cooldownKey(email, purpose));
    return Boolean.TRUE.equals(exists);
  }

  @Override
  public void saveCooldown(String email, OtpPurpose purpose, Duration ttl) {
    redisTemplate.opsForValue().set(cooldownKey(email, purpose), "1", ttl);
  }

  @Override
  public long getCooldownSeconds(String email, OtpPurpose purpose) {
    Long ttl = redisTemplate.getExpire(cooldownKey(email, purpose));
    return ttl == null || ttl < 0 ? 0 : ttl;
  }

  private String codeKey(String email, OtpPurpose purpose) {
    return "otp:code:" + purpose.name() + ":" + email;
  }

  private String attemptKey(String email, OtpPurpose purpose) {
    return "otp:attempts:" + purpose.name() + ":" + email;
  }

  private String cooldownKey(String email, OtpPurpose purpose) {
    return "otp:cooldown:" + purpose.name() + ":" + email;
  }
}
