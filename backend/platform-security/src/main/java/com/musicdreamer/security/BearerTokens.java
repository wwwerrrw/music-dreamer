package com.musicdreamer.security;

import java.util.Optional;

public final class BearerTokens {

    private static final String SCHEME = "Bearer";

    private BearerTokens() {
    }

    public static Optional<String> extract(String authorization) {
        if (authorization == null) {
            return Optional.empty();
        }
        int separator = authorization.indexOf(' ');
        if (separator < 1 || !authorization.substring(0, separator).equalsIgnoreCase(SCHEME)) {
            return Optional.empty();
        }
        String token = authorization.substring(separator + 1).trim();
        if (token.isEmpty() || token.chars().anyMatch(Character::isWhitespace)) {
            return Optional.empty();
        }
        return Optional.of(token);
    }
}
