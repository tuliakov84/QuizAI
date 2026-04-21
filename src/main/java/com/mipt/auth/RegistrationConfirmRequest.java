package com.mipt.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record RegistrationConfirmRequest(
    @NotBlank @Email String email,
    @NotBlank @Pattern(regexp = "^\\d{4,8}$") String code
) {
}
