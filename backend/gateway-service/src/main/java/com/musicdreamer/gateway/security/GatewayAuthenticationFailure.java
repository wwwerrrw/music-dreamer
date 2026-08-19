package com.musicdreamer.gateway.security;

import java.util.Objects;

public final class GatewayAuthenticationFailure extends RuntimeException {

    public enum Reason {
        SESSION_INVALID,
        SESSION_STORE_UNAVAILABLE
    }

    private final Reason reason;

    public GatewayAuthenticationFailure(Reason reason) {
        super(reason.name());
        this.reason = Objects.requireNonNull(reason, "reason");
    }

    public GatewayAuthenticationFailure(Reason reason, Throwable cause) {
        super(reason.name(), cause);
        this.reason = Objects.requireNonNull(reason, "reason");
    }

    public Reason reason() {
        return reason;
    }
}
