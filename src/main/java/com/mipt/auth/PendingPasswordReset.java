package com.mipt.auth;

public record PendingPasswordReset(
    String email,
    String passwordHash
) {
}
