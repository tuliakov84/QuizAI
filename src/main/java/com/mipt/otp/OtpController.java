package com.mipt.otp;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/otp")
public class OtpController {
  private final OtpService otpService;

  public OtpController(OtpService otpService) {
    this.otpService = otpService;
  }

  @PostMapping("/send")
  public ResponseEntity<OtpResponse> send(@Valid @RequestBody OtpSendRequest request) {
    return ResponseEntity.ok(otpService.sendOtp(request));
  }

  @PostMapping("/verify")
  public ResponseEntity<OtpResponse> verify(@Valid @RequestBody OtpVerifyRequest request) {
    return ResponseEntity.ok(otpService.verifyOtp(request));
  }

  @PostMapping("/test-send")
  public ResponseEntity<OtpResponse> testSend(@Valid @RequestBody OtpSendRequest request) {
    return ResponseEntity.ok(otpService.sendStubOtp(request));
  }

  @ExceptionHandler(OtpException.class)
  public ResponseEntity<OtpResponse> handleOtpException(OtpException exception) {
    return ResponseEntity.status(exception.getStatus())
        .body(OtpResponse.error(null, null, exception.getMessage(), exception.getRetryAfterSeconds()));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<OtpResponse> handleValidationException(MethodArgumentNotValidException exception) {
    String errorMessage = exception.getBindingResult()
        .getFieldErrors()
        .stream()
        .map(FieldError::getDefaultMessage)
        .collect(Collectors.joining("; "));

    return ResponseEntity.badRequest()
        .body(OtpResponse.error(null, null, errorMessage, null));
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<OtpResponse> handleIllegalArgumentException(IllegalArgumentException exception) {
    return ResponseEntity.badRequest()
        .body(OtpResponse.error(null, null, exception.getMessage(), null));
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<OtpResponse> handleUnreadableException(HttpMessageNotReadableException exception) {
    return ResponseEntity.badRequest()
        .body(OtpResponse.error(null, null, "Invalid request body or unsupported OTP purpose", null));
  }
}
