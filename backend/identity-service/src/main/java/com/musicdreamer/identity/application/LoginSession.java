package com.musicdreamer.identity.application;

import com.musicdreamer.security.AccountStatus;
import com.musicdreamer.security.Role;
import java.time.Instant;
import java.util.Objects;

public record LoginSession(
        String token,
        Instant expiresAt,
        long userId,
        Role role,
        AccountStatus status) {

    public LoginSession {
        Objects.requireNonNull(token, "token");
        Objects.requireNonNull(expiresAt, "expiresAt");
        Objects.requireNonNull(role, "role");
        Objects.requireNonNull(status, "status");
    }
}
