package com.musicdreamer.gateway.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.jayway.jsonpath.JsonPath;
import com.musicdreamer.security.AccountStatus;
import com.musicdreamer.security.AuthenticatedPrincipal;
import com.musicdreamer.security.Role;
import com.musicdreamer.security.TrustedIdentityHeaders;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

class GatewaySecurityFilterTest {

    @Test
    void mediaObjectGetWithoutBearerUsesTheMediaSessionCookie() {
        GatewaySecurityFilter filter = new GatewaySecurityFilter(new GatewayAuthenticationService(token ->
                token.equals("valid-media-session")
                        ? Mono.just(new AuthenticatedPrincipal(17L, Role.USER, AccountStatus.ACTIVE))
                        : Mono.empty(),
                rawToken -> "trusted-media-session-digest"));
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest
                .get("/api/media/objects/42")
                .cookie(new HttpCookie("music_dreamer_media_session", "valid-media-session"))
                .build());
        AtomicReference<ServerWebExchange> downstream = new AtomicReference<>();

        filter.filter(exchange, downstreamRecordingChain(downstream)).block(Duration.ofSeconds(2));

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.OK);
        HttpHeaders forwarded = downstream.get().getRequest().getHeaders();
        assertThat(forwarded.getFirst(TrustedIdentityHeaders.USER_ID)).isEqualTo("17");
        assertThat(forwarded.getFirst(TrustedIdentityHeaders.SESSION_DIGEST))
                .isEqualTo("trusted-media-session-digest");
    }

    @Test
    void mediaSessionCookieCannotAuthenticateAnotherApiPath() {
        GatewaySecurityFilter filter = new GatewaySecurityFilter(new GatewayAuthenticationService(token ->
                Mono.just(new AuthenticatedPrincipal(17L, Role.USER, AccountStatus.ACTIVE)),
                rawToken -> "trusted-media-session-digest"));
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest
                .get("/api/catalog/tracks")
                .cookie(new HttpCookie("music_dreamer_media_session", "valid-media-session"))
                .build());
        AtomicReference<ServerWebExchange> downstream = new AtomicReference<>();

        filter.filter(exchange, downstreamRecordingChain(downstream)).block(Duration.ofSeconds(2));

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(downstream).hasValue(null);
    }

    @Test
    void mediaSessionCookieCannotAuthenticateAMediaWrite() {
        GatewaySecurityFilter filter = new GatewaySecurityFilter(new GatewayAuthenticationService(token ->
                Mono.just(new AuthenticatedPrincipal(17L, Role.SINGER, AccountStatus.ACTIVE)),
                rawToken -> "trusted-media-session-digest"));
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest
                .post("/api/media/objects")
                .cookie(new HttpCookie("music_dreamer_media_session", "valid-media-session"))
                .build());
        AtomicReference<ServerWebExchange> downstream = new AtomicReference<>();

        filter.filter(exchange, downstreamRecordingChain(downstream)).block(Duration.ofSeconds(2));

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(downstream).hasValue(null);
    }

    @Test
    void invalidBearerDoesNotFallBackToAValidMediaSessionCookie() {
        GatewaySecurityFilter filter = new GatewaySecurityFilter(new GatewayAuthenticationService(token ->
                token.equals("valid-media-session")
                        ? Mono.just(new AuthenticatedPrincipal(17L, Role.USER, AccountStatus.ACTIVE))
                        : Mono.empty(),
                rawToken -> "trusted-media-session-digest"));
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest
                .get("/api/media/objects/42")
                .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-bearer")
                .cookie(new HttpCookie("music_dreamer_media_session", "valid-media-session"))
                .build());
        AtomicReference<ServerWebExchange> downstream = new AtomicReference<>();

        filter.filter(exchange, downstreamRecordingChain(downstream)).block(Duration.ofSeconds(2));

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(downstream).hasValue(null);
    }

    @Test
    void protectedRequestWithoutBearerSessionReturnsProblemDetailWithoutCallingDownstream() {
        GatewaySecurityFilter filter = new GatewaySecurityFilter(new GatewayAuthenticationService(
                token -> Mono.empty(),
                rawToken -> "unused-digest"));
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest
                .get("/api/catalog/tracks")
                .header("X-Request-Id", "trace-gateway-2")
                .build());
        AtomicReference<ServerWebExchange> downstream = new AtomicReference<>();

        filter.filter(exchange, downstreamRecordingChain(downstream)).block(Duration.ofSeconds(2));

        assertThat(downstream).hasValue(null);
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(exchange.getResponse().getHeaders().getContentType())
                .isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
        String responseBody = exchange.getResponse().getBodyAsString().block(Duration.ofSeconds(2));
        assertThat(JsonPath.<String>read(responseBody, "$.code")).isEqualTo("IDENTITY_SESSION_INVALID");
        assertThat(JsonPath.<String>read(responseBody, "$.traceId")).isEqualTo("trace-gateway-2");
    }

    @Test
    void resolvedSessionReplacesForgedHeadersOnTheForwardedExchange() {
        GatewaySecurityFilter filter = new GatewaySecurityFilter(new GatewayAuthenticationService(token ->
                Mono.just(new AuthenticatedPrincipal(17L, Role.USER, AccountStatus.ACTIVE)),
                rawToken -> "trusted-session-digest"));
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest
                .get("/api/catalog/tracks")
                .header(HttpHeaders.AUTHORIZATION, "Bearer raw-session-token")
                .header(TrustedIdentityHeaders.USER_ID, "999")
                .header(TrustedIdentityHeaders.ROLE, Role.ADMIN.name())
                .build());
        AtomicReference<ServerWebExchange> downstream = new AtomicReference<>();

        filter.filter(exchange, downstreamRecordingChain(downstream)).block(Duration.ofSeconds(2));

        HttpHeaders forwarded = downstream.get().getRequest().getHeaders();
        assertThat(forwarded.getFirst(HttpHeaders.AUTHORIZATION)).isNull();
        assertThat(forwarded.getFirst(TrustedIdentityHeaders.USER_ID)).isEqualTo("17");
        assertThat(forwarded.getFirst(TrustedIdentityHeaders.ROLE)).isEqualTo("USER");
        assertThat(forwarded.getFirst(TrustedIdentityHeaders.ACCOUNT_STATUS)).isEqualTo("ACTIVE");
        assertThat(forwarded.getFirst(TrustedIdentityHeaders.SESSION_DIGEST))
                .isEqualTo("trusted-session-digest");
    }

    @Test
    void publicLoginRouteSkipsSessionResolutionButStillStripsUntrustedHeaders() {
        GatewaySecurityFilter filter = new GatewaySecurityFilter(new GatewayAuthenticationService(token -> {
            throw new AssertionError("public route must not resolve a session");
        }, rawToken -> {
            throw new AssertionError("public route must not digest a session token");
        }));
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest
                .post("/api/identity/sessions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer forged-session")
                .header(TrustedIdentityHeaders.USER_ID, "999")
                .header(TrustedIdentityHeaders.ROLE, Role.ADMIN.name())
                .header("X-Music-Dreamer-Session-Digest", "forged-session-digest")
                .build());
        AtomicReference<ServerWebExchange> downstream = new AtomicReference<>();

        filter.filter(exchange, downstreamRecordingChain(downstream)).block(Duration.ofSeconds(2));

        HttpHeaders forwarded = downstream.get().getRequest().getHeaders();
        assertThat(forwarded.getFirst(HttpHeaders.AUTHORIZATION)).isNull();
        assertThat(forwarded.getFirst(TrustedIdentityHeaders.USER_ID)).isNull();
        assertThat(forwarded.getFirst(TrustedIdentityHeaders.ROLE)).isNull();
        assertThat(forwarded.getFirst(TrustedIdentityHeaders.ACCOUNT_STATUS)).isNull();
        assertThat(forwarded.getFirst(TrustedIdentityHeaders.SESSION_DIGEST)).isNull();
    }

    @Test
    void unavailableSessionStoreReturnsStableProblemDetailWithoutCallingDownstream() {
        GatewaySecurityFilter filter = new GatewaySecurityFilter(new GatewayAuthenticationService(
                token -> Mono.error(new RedisConnectionFailureException("redis endpoint unavailable")),
                rawToken -> "unused-digest"));
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest
                .get("/api/catalog/tracks")
                .header(HttpHeaders.AUTHORIZATION, "Bearer raw-session-token")
                .header("X-Request-Id", "trace-gateway-503")
                .build());
        AtomicReference<ServerWebExchange> downstream = new AtomicReference<>();

        filter.filter(exchange, downstreamRecordingChain(downstream)).block(Duration.ofSeconds(2));

        assertThat(downstream).hasValue(null);
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(exchange.getResponse().getHeaders().getContentType())
                .isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
        String responseBody = exchange.getResponse().getBodyAsString().block(Duration.ofSeconds(2));
        assertThat(JsonPath.<String>read(responseBody, "$.code"))
                .isEqualTo("IDENTITY_SESSION_STORE_UNAVAILABLE");
        assertThat(JsonPath.<String>read(responseBody, "$.traceId"))
                .isEqualTo("trace-gateway-503");
        assertThat(JsonPath.<String>read(responseBody, "$.detail"))
                .isEqualTo("Session service is temporarily unavailable");
    }

    @Test
    void downstreamDataAccessFailureIsNotMisreportedAsSessionStoreFailure() {
        GatewaySecurityFilter filter = new GatewaySecurityFilter(new GatewayAuthenticationService(token ->
                Mono.just(new AuthenticatedPrincipal(17L, Role.USER, AccountStatus.ACTIVE)),
                rawToken -> "trusted-session-digest"));
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest
                .get("/api/catalog/tracks")
                .header(HttpHeaders.AUTHORIZATION, "Bearer raw-session-token")
                .build());
        DataRetrievalFailureException downstreamFailure =
                new DataRetrievalFailureException("catalog database unavailable");

        assertThatThrownBy(() -> filter.filter(
                        exchange,
                        forwarded -> Mono.error(downstreamFailure))
                .block(Duration.ofSeconds(2)))
                .isSameAs(downstreamFailure);
        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    private static GatewayFilterChain downstreamRecordingChain(
            AtomicReference<ServerWebExchange> downstream) {
        return exchange -> {
            downstream.set(exchange);
            exchange.getResponse().setStatusCode(HttpStatus.OK);
            return exchange.getResponse().setComplete();
        };
    }
}
