package com.musicdreamer.identity.domain;

import java.time.Instant;
import java.util.Objects;

public record ActivationToken(
        long id,
        long userId,
        String digest,
        Instant expiresAt,
        Instant usedAt,
        long version) {

    public ActivationToken {
        Objects.requireNonNull(digest, "digest");
        Objects.requireNonNull(expiresAt, "expiresAt");
    }

    public ActivationToken markUsed(Instant usedAt) {
        return new ActivationToken(id, userId, digest, expiresAt, usedAt, version + 1);
    }
}
