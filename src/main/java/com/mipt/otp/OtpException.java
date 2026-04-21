package com.mipt.otp;

import org.springframework.http.HttpStatus;

public class OtpException extends RuntimeException {
  private final HttpStatus status;
  private final Long retryAfterSeconds;

  public OtpException(String message, Throwable cause) {
    super(message, cause);
    this.status = HttpStatus.INTERNAL_SERVER_ERROR;
    this.retryAfterSeconds = null;
  }

  public OtpException(HttpStatus status, String message) {
    this(status, message, null);
  }

  public OtpException(HttpStatus status, String message, Long retryAfterSeconds) {
    super(message);
    this.status = status;
    this.retryAfterSeconds = retryAfterSeconds;
  }

  public HttpStatus getStatus() {
    return status;
  }

  public Long getRetryAfterSeconds() {
    return retryAfterSeconds;
  }
}
