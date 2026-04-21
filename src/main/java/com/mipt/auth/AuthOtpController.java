package com.mipt.auth;

import com.mipt.otp.OtpException;
import com.mipt.otp.OtpResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
public class AuthOtpController {
  private final AuthOtpFlowService authOtpFlowService;

  public AuthOtpController(AuthOtpFlowService authOtpFlowService) {
    this.authOtpFlowService = authOtpFlowService;
  }

  @PostMapping("/register/request-otp")
  public ResponseEntity<OtpResponse> requestRegistrationOtp(@RequestBody RegistrationStartRequest request) {
    return ResponseEntity.ok(authOtpFlowService.startRegistration(request));
  }

  @PostMapping("/register/confirm")
  public ResponseEntity<AuthActionResponse> confirmRegistration(@Valid @RequestBody RegistrationConfirmRequest request) {
    return ResponseEntity.ok(authOtpFlowService.confirmRegistration(request));
  }

  @PostMapping("/password-reset/request-otp")
  public ResponseEntity<OtpResponse> requestPasswordResetOtp(@Valid @RequestBody PasswordResetStartRequest request) {
    return ResponseEntity.ok(authOtpFlowService.startPasswordReset(request));
  }

  @PostMapping("/password-reset/confirm")
  public ResponseEntity<AuthActionResponse> confirmPasswordReset(@Valid @RequestBody PasswordResetConfirmRequest request) {
    return ResponseEntity.ok(authOtpFlowService.confirmPasswordReset(request));
  }

  @ExceptionHandler(OtpException.class)
  public ResponseEntity<AuthActionResponse> handleOtpException(OtpException exception) {
    return ResponseEntity.status(exception.getStatus())
        .body(AuthActionResponse.success("AUTH_FLOW_ERROR", exception.getMessage(), null));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<AuthActionResponse> handleValidationException(MethodArgumentNotValidException exception) {
    String errorMessage = exception.getBindingResult()
        .getFieldErrors()
        .stream()
        .map(FieldError::getDefaultMessage)
        .collect(Collectors.joining("; "));

    return ResponseEntity.badRequest()
        .body(AuthActionResponse.success("AUTH_FLOW_ERROR", errorMessage, null));
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<AuthActionResponse> handleUnreadableException(HttpMessageNotReadableException exception) {
    return ResponseEntity.badRequest()
        .body(AuthActionResponse.success("AUTH_FLOW_ERROR", "Invalid request body", null));
  }
}
