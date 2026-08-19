package com.musicdreamer.gateway.security;

import com.musicdreamer.security.TrustedIdentityHeaders;
import java.util.Objects;
import java.util.function.Function;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpHeaders;
import reactor.core.publisher.Mono;

public final class GatewayAuthenticationService {

    private final SessionResolver sessions;
    private final Function<String, String> tokenDigester;

    public GatewayAuthenticationService(
            SessionResolver sessions,
            Function<String, String> tokenDigester) {
        this.sessions = Objects.requireNonNull(sessions, "sessions");
        this.tokenDigester = Objects.requireNonNull(tokenDigester, "tokenDigester");
    }

    public Mono<HttpHeaders> authenticate(String sessionToken, HttpHeaders clientHeaders) {
        return sessions.resolve(sessionToken)
                .onErrorMap(DataAccessException.class, failure -> new GatewayAuthenticationFailure(
                        GatewayAuthenticationFailure.Reason.SESSION_STORE_UNAVAILABLE,
                        failure))
                .switchIfEmpty(Mono.error(() -> new GatewayAuthenticationFailure(
                        GatewayAuthenticationFailure.Reason.SESSION_INVALID)))
                .map(principal -> TrustedIdentityHeaders.forPrincipal(
                        clientHeaders,
                        principal,
                        tokenDigester.apply(sessionToken)));
    }
}
