package com.musicdreamer.security;

import java.util.Objects;
import java.util.Optional;
import org.springframework.http.HttpHeaders;

public final class TrustedIdentityHeaders {

    public static final String USER_ID = "X-Music-Dreamer-User-Id";
    public static final String ROLE = "X-Music-Dreamer-Role";
    public static final String ACCOUNT_STATUS = "X-Music-Dreamer-Account-Status";
    public static final String SESSION_DIGEST = "X-Music-Dreamer-Session-Digest";

    private TrustedIdentityHeaders() {
    }

    public static HttpHeaders sanitize(HttpHeaders clientHeaders) {
        Objects.requireNonNull(clientHeaders, "clientHeaders");
        HttpHeaders trustedHeaders = new HttpHeaders();
        trustedHeaders.addAll(clientHeaders);
        trustedHeaders.remove(HttpHeaders.AUTHORIZATION);
        trustedHeaders.remove(USER_ID);
        trustedHeaders.remove(ROLE);
        trustedHeaders.remove(ACCOUNT_STATUS);
        trustedHeaders.remove(SESSION_DIGEST);
        return trustedHeaders;
    }

    public static HttpHeaders forPrincipal(HttpHeaders clientHeaders, AuthenticatedPrincipal principal) {
        return forPrincipal(clientHeaders, principal, null);
    }

    public static HttpHeaders forPrincipal(
            HttpHeaders clientHeaders,
            AuthenticatedPrincipal principal,
            String sessionDigest) {
        Objects.requireNonNull(principal, "principal");
        HttpHeaders trustedHeaders = sanitize(clientHeaders);
        trustedHeaders.set(USER_ID, Long.toString(principal.userId()));
        trustedHeaders.set(ROLE, principal.role().name());
        trustedHeaders.set(ACCOUNT_STATUS, principal.status().name());
        if (sessionDigest != null) {
            trustedHeaders.set(SESSION_DIGEST, sessionDigest);
        }
        return trustedHeaders;
    }

    public static Optional<AuthenticatedPrincipal> principalFrom(HttpHeaders headers) {
        Objects.requireNonNull(headers, "headers");
        try {
            return Optional.of(new AuthenticatedPrincipal(
                    Long.parseLong(headers.getFirst(USER_ID)),
                    Role.valueOf(headers.getFirst(ROLE)),
                    AccountStatus.valueOf(headers.getFirst(ACCOUNT_STATUS))));
        } catch (IllegalArgumentException | NullPointerException invalidHeaders) {
            return Optional.empty();
        }
    }
}
