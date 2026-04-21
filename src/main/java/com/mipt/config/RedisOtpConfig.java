package com.mipt.config;

import com.mipt.otp.OtpProperties;
import com.mipt.otp.RedisOtpProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
@EnableConfigurationProperties({RedisOtpProperties.class, OtpProperties.class})
public class RedisOtpConfig {

  @Bean
  public LettuceConnectionFactory redisConnectionFactory(RedisOtpProperties properties) {
    RedisStandaloneConfiguration configuration =
        new RedisStandaloneConfiguration(properties.getHost(), properties.getPort());
    configuration.setDatabase(properties.getDatabase());

    if (properties.getPassword() != null && !properties.getPassword().isBlank()) {
      configuration.setPassword(RedisPassword.of(properties.getPassword()));
    }

    return new LettuceConnectionFactory(configuration);
  }

  @Bean
  public StringRedisTemplate stringRedisTemplate(LettuceConnectionFactory redisConnectionFactory) {
    return new StringRedisTemplate(redisConnectionFactory);
  }
}
