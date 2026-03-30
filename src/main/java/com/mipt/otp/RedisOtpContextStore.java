package com.mipt.otp;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Component
public class RedisOtpContextStore implements OtpContextStore {
  private final StringRedisTemplate redisTemplate;

  public RedisOtpContextStore(StringRedisTemplate redisTemplate) {
    this.redisTemplate = redisTemplate;
  }

  @Override
  public void saveContext(String email, OtpPurpose purpose, String payload, Duration ttl) {
    redisTemplate.opsForValue().set(contextKey(email, purpose), payload, ttl);
  }

  @Override
  public Optional<String> getContext(String email, OtpPurpose purpose) {
    return Optional.ofNullable(redisTemplate.opsForValue().get(contextKey(email, purpose)));
  }

  @Override
  public void clearContext(String email, OtpPurpose purpose) {
    redisTemplate.delete(contextKey(email, purpose));
  }

  private String contextKey(String email, OtpPurpose purpose) {
    return "otp:context:" + purpose.name() + ":" + email;
  }
}
