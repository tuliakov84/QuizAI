package com.mipt.otp;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.otp")
public class OtpProperties {
  private long ttlSeconds = 300;
  private long resendCooldownSeconds = 60;
  private int maxVerificationAttempts = 5;
  private int codeLength = 6;
  private boolean testEndpointEnabled = true;

  public long getTtlSeconds() {
    return ttlSeconds;
  }

  public void setTtlSeconds(long ttlSeconds) {
    this.ttlSeconds = ttlSeconds;
  }

  public long getResendCooldownSeconds() {
    return resendCooldownSeconds;
  }

  public void setResendCooldownSeconds(long resendCooldownSeconds) {
    this.resendCooldownSeconds = resendCooldownSeconds;
  }

  public int getMaxVerificationAttempts() {
    return maxVerificationAttempts;
  }

  public void setMaxVerificationAttempts(int maxVerificationAttempts) {
    this.maxVerificationAttempts = maxVerificationAttempts;
  }

  public int getCodeLength() {
    return codeLength;
  }

  public void setCodeLength(int codeLength) {
    this.codeLength = codeLength;
  }

  public boolean isTestEndpointEnabled() {
    return testEndpointEnabled;
  }

  public void setTestEndpointEnabled(boolean testEndpointEnabled) {
    this.testEndpointEnabled = testEndpointEnabled;
  }
}
