package com.musicdreamer.identity.inbox;

import java.time.Instant;
import java.util.Objects;

public record DevelopmentInboxMessage(String email, String rawToken, Instant expiresAt) {

    public DevelopmentInboxMessage {
        Objects.requireNonNull(email, "email");
        Objects.requireNonNull(rawToken, "rawToken");
        Objects.requireNonNull(expiresAt, "expiresAt");
    }
}
