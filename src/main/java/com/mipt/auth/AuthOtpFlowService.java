package com.mipt.auth;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mipt.dbAPI.DatabaseAccessException;
import com.mipt.dbAPI.DbService;
import com.mipt.otp.OtpContextStore;
import com.mipt.otp.OtpException;
import com.mipt.otp.OtpProperties;
import com.mipt.otp.OtpPurpose;
import com.mipt.otp.OtpResponse;
import com.mipt.otp.OtpSendRequest;
import com.mipt.otp.OtpService;
import com.mipt.otp.OtpVerifyRequest;
import com.mipt.utils.ValidationUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.time.Duration;
import java.util.Locale;

@Service
public class AuthOtpFlowService {
  private final DbService dbService;
  private final OtpService otpService;
  private final OtpContextStore otpContextStore;
  private final OtpProperties otpProperties;
  private final ObjectMapper objectMapper;

  public AuthOtpFlowService(
      DbService dbService,
      OtpService otpService,
      OtpContextStore otpContextStore,
      OtpProperties otpProperties,
      ObjectMapper objectMapper
  ) {
    this.dbService = dbService;
    this.otpService = otpService;
    this.otpContextStore = otpContextStore;
    this.otpProperties = otpProperties;
    this.objectMapper = objectMapper;
  }

  public OtpResponse startRegistration(RegistrationStartRequest request) {
    String username = request.username().trim();
    String email = normalizeEmail(request.email());
    validateRegistration(username, request.password(), email);

    PendingRegistration pendingRegistration = new PendingRegistration(
        username,
        email,
        hashPassword(request.password())
    );

    saveContext(email, OtpPurpose.REGISTRATION, pendingRegistration);
    try {
      return otpService.sendOtp(new OtpSendRequest(email, OtpPurpose.REGISTRATION));
    } catch (RuntimeException exception) {
      otpContextStore.clearContext(email, OtpPurpose.REGISTRATION);
      throw exception;
    }
  }

  public AuthActionResponse confirmRegistration(RegistrationConfirmRequest request) {
    String email = normalizeEmail(request.email());
    PendingRegistration pendingRegistration = readContext(email, OtpPurpose.REGISTRATION, PendingRegistration.class);

    otpService.verifyOtp(new OtpVerifyRequest(email, OtpPurpose.REGISTRATION, request.code()));

    try {
      dbService.registerHashedPassword(
          pendingRegistration.username(),
          pendingRegistration.passwordHash(),
          pendingRegistration.email()
      );
      otpContextStore.clearContext(email, OtpPurpose.REGISTRATION);
      return AuthActionResponse.success(
          "REGISTRATION_COMPLETED",
          "Registration completed successfully. You can now log in.",
          email
      );
    } catch (SQLException e) {
      throw new OtpException(HttpStatus.INTERNAL_SERVER_ERROR, "Database error while completing registration");
    } catch (DatabaseAccessException e) {
      otpContextStore.clearContext(email, OtpPurpose.REGISTRATION);
      throw new OtpException(HttpStatus.BAD_REQUEST, e.getMessage());
    }
  }

  public OtpResponse startPasswordReset(PasswordResetStartRequest request) {
    String email = normalizeEmail(request.email());
    validatePasswordReset(request.password(), email);

    PendingPasswordReset pendingPasswordReset = new PendingPasswordReset(
        email,
        hashPassword(request.password())
    );

    saveContext(email, OtpPurpose.PASSWORD_RESET, pendingPasswordReset);
    try {
      return otpService.sendOtp(new OtpSendRequest(email, OtpPurpose.PASSWORD_RESET));
    } catch (RuntimeException exception) {
      otpContextStore.clearContext(email, OtpPurpose.PASSWORD_RESET);
      throw exception;
    }
  }

  public AuthActionResponse confirmPasswordReset(PasswordResetConfirmRequest request) {
    String email = normalizeEmail(request.email());
    PendingPasswordReset pendingPasswordReset = readContext(email, OtpPurpose.PASSWORD_RESET, PendingPasswordReset.class);

    otpService.verifyOtp(new OtpVerifyRequest(email, OtpPurpose.PASSWORD_RESET, request.code()));

    try {
      dbService.changePasswordHashByEmail(email, pendingPasswordReset.passwordHash());
      otpContextStore.clearContext(email, OtpPurpose.PASSWORD_RESET);
      return AuthActionResponse.success(
          "PASSWORD_RESET_COMPLETED",
          "Password updated successfully. You can now log in with the new password.",
          email
      );
    } catch (SQLException e) {
      throw new OtpException(HttpStatus.INTERNAL_SERVER_ERROR, "Database error while resetting password");
    } catch (DatabaseAccessException e) {
      otpContextStore.clearContext(email, OtpPurpose.PASSWORD_RESET);
      throw new OtpException(HttpStatus.BAD_REQUEST, e.getMessage());
    }
  }

  private void validateRegistration(String username, String password, String email) {
    if (!ValidationUtils.usernameValidation(username)) {
      throw new OtpException(HttpStatus.BAD_REQUEST, "Username validation error. Bad username");
    }
    if (!ValidationUtils.passwordValidation(password)) {
      throw new OtpException(HttpStatus.BAD_REQUEST, "Password validation error. Bad password");
    }
    if (!ValidationUtils.emailValidation(email)) {
      throw new OtpException(HttpStatus.BAD_REQUEST, "Email validation error. Bad email");
    }

    try {
      if (dbService.checkUserExists(username)) {
        throw new OtpException(HttpStatus.BAD_REQUEST, "User already exists");
      }
      if (dbService.checkEmailExists(email)) {
        throw new OtpException(HttpStatus.BAD_REQUEST, "Email already exists");
      }
    } catch (SQLException e) {
      throw new OtpException(HttpStatus.INTERNAL_SERVER_ERROR, "Database error while preparing registration");
    }
  }

  private void validatePasswordReset(String password, String email) {
    if (!ValidationUtils.passwordValidation(password)) {
      throw new OtpException(HttpStatus.BAD_REQUEST, "Password validation error. Bad password");
    }
    if (!ValidationUtils.emailValidation(email)) {
      throw new OtpException(HttpStatus.BAD_REQUEST, "Email validation error. Bad email");
    }

    try {
      if (!dbService.userExistsByEmail(email)) {
        throw new OtpException(HttpStatus.BAD_REQUEST, "User with this email does not exist");
      }
    } catch (SQLException e) {
      throw new OtpException(HttpStatus.INTERNAL_SERVER_ERROR, "Database error while preparing password reset");
    }
  }

  private String hashPassword(String password) {
    return BCrypt.withDefaults().hashToString(12, password.toCharArray());
  }

  private void saveContext(String email, OtpPurpose purpose, Object payload) {
    try {
      otpContextStore.saveContext(
          email,
          purpose,
          objectMapper.writeValueAsString(payload),
          Duration.ofSeconds(otpProperties.getTtlSeconds())
      );
    } catch (JsonProcessingException e) {
      throw new OtpException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to prepare server-side OTP request");
    }
  }

  private <T> T readContext(String email, OtpPurpose purpose, Class<T> payloadType) {
    String payload = otpContextStore.getContext(email, purpose)
        .orElseThrow(() -> new OtpException(HttpStatus.BAD_REQUEST, "No active server-side request for this operation"));

    try {
      return objectMapper.readValue(payload, payloadType);
    } catch (JsonProcessingException e) {
      throw new OtpException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to read server-side OTP request");
    }
  }

  private String normalizeEmail(String email) {
    return email.trim().toLowerCase(Locale.ROOT);
  }
}
