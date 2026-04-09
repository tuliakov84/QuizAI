package com.mipt.auth;

public record PendingRegistration(
    String username,
    String email,
    String passwordHash
) {
}
