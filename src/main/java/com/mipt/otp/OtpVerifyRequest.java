package com.mipt.otp;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record OtpVerifyRequest(
    @NotBlank @Email String email,
    @NotNull OtpPurpose purpose,
    @NotBlank @Pattern(regexp = "^\\d{4,8}$") String code
) {
}
