package com.musicdreamer.identity.application;

import java.util.Objects;

public final class IdentityFailure extends RuntimeException {

    public enum Reason {
        ACCOUNT_NOT_ACTIVE,
        ACCOUNT_LOCKED,
        INVALID_CREDENTIALS,
        SESSION_INVALID,
        REGISTRATION_CONFLICT,
        FORBIDDEN,
        USER_NOT_FOUND,
        ROLE_CONFLICT,
        ACTIVATION_TOKEN_INVALID,
        ACTIVATION_TOKEN_EXPIRED,
        ACTIVATION_TOKEN_USED
    }

    private final Reason reason;

    public IdentityFailure(Reason reason) {
        super(reason.name());
        this.reason = Objects.requireNonNull(reason, "reason");
    }

    public Reason reason() {
        return reason;
    }
}
