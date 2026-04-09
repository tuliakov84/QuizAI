package com.mipt.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RegistrationStartRequest(
    @NotBlank String username,
    @NotBlank @Email String email,
    @NotBlank String password
) {
}
