package com.musicdreamer.gateway.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.musicdreamer.contracts.error.ApiErrorCode;
import com.musicdreamer.contracts.error.ProblemDetails;
import com.musicdreamer.security.BearerTokens;
import com.musicdreamer.security.TrustedIdentityHeaders;
import java.util.Objects;
import java.util.UUID;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.json.ProblemDetailJacksonMixin;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

public final class GatewaySecurityFilter implements GlobalFilter, Ordered {

    private static final String MEDIA_SESSION_COOKIE = "music_dreamer_media_session";

    private final GatewayAuthenticationService authentication;
    private final ObjectMapper objectMapper;

    public GatewaySecurityFilter(GatewayAuthenticationService authentication) {
        this(authentication, new ObjectMapper().findAndRegisterModules());
    }

    public GatewaySecurityFilter(
            GatewayAuthenticationService authentication,
            ObjectMapper objectMapper) {
        this.authentication = Objects.requireNonNull(authentication, "authentication");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper")
                .copy()
                .addMixIn(ProblemDetail.class, ProblemDetailJacksonMixin.class);
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (isPublic(exchange)) {
            return forwardWithHeaders(
                    exchange,
                    chain,
                    TrustedIdentityHeaders.sanitize(exchange.getRequest().getHeaders()));
        }
        String rawToken = BearerTokens.extract(
                        exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION))
                .orElse(null);
        if (rawToken == null && isMediaObjectGet(exchange)) {
            var mediaCookie = exchange.getRequest().getCookies().getFirst(MEDIA_SESSION_COOKIE);
            rawToken = mediaCookie == null ? null : mediaCookie.getValue();
        }
        if (rawToken == null) {
            return unauthorized(exchange);
        }
        return authentication.authenticate(rawToken, exchange.getRequest().getHeaders())
                .flatMap(trustedHeaders -> forwardWithHeaders(exchange, chain, trustedHeaders))
                .onErrorResume(GatewayAuthenticationFailure.class, failure ->
                        switch (failure.reason()) {
                            case SESSION_INVALID -> unauthorized(exchange);
                            case SESSION_STORE_UNAVAILABLE -> sessionStoreUnavailable(exchange);
                        });
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        return writeProblem(exchange, ProblemDetails.create(
                HttpStatus.UNAUTHORIZED,
                ApiErrorCode.IDENTITY_SESSION_INVALID,
                "Session is missing or invalid",
                traceId(exchange)));
    }

    private Mono<Void> sessionStoreUnavailable(ServerWebExchange exchange) {
        return writeProblem(exchange, ProblemDetails.create(
                HttpStatus.SERVICE_UNAVAILABLE,
                ApiErrorCode.IDENTITY_SESSION_STORE_UNAVAILABLE,
                "Session service is temporarily unavailable",
                traceId(exchange)));
    }

    private Mono<Void> writeProblem(ServerWebExchange exchange, ProblemDetail problem) {
        byte[] body;
        try {
            body = objectMapper.writeValueAsBytes(problem);
        } catch (JsonProcessingException serializationFailure) {
            return Mono.error(serializationFailure);
        }
        var response = exchange.getResponse();
        response.setStatusCode(HttpStatus.valueOf(problem.getStatus()));
        response.getHeaders().setContentType(MediaType.APPLICATION_PROBLEM_JSON);
        response.getHeaders().setContentLength(body.length);
        return response.writeWith(Mono.just(response.bufferFactory().wrap(body)));
    }

    private static Mono<Void> forwardWithHeaders(
            ServerWebExchange exchange,
            GatewayFilterChain chain,
            HttpHeaders forwardedHeaders) {
        var forwardedRequest = exchange.getRequest().mutate()
                .headers(headers -> {
                    headers.clear();
                    headers.putAll(forwardedHeaders);
                })
                .build();
        return chain.filter(exchange.mutate().request(forwardedRequest).build());
    }

    private static boolean isPublic(ServerWebExchange exchange) {
        HttpMethod method = exchange.getRequest().getMethod();
        String path = exchange.getRequest().getPath().value();
        if (method == HttpMethod.POST) {
            return path.equals("/api/identity/registrations")
                    || path.equals("/api/identity/activations")
                    || path.equals("/api/identity/sessions");
        }
        return method == HttpMethod.GET && path.equals("/api/identity/development-inbox");
    }

    private static boolean isMediaObjectGet(ServerWebExchange exchange) {
        String path = exchange.getRequest().getPath().value();
        return exchange.getRequest().getMethod() == HttpMethod.GET
                && (path.equals("/api/media/objects") || path.startsWith("/api/media/objects/"));
    }

    private static String traceId(ServerWebExchange exchange) {
        String requestId = exchange.getRequest().getHeaders().getFirst("X-Request-Id");
        return requestId == null || requestId.isBlank()
                ? UUID.randomUUID().toString()
                : requestId;
    }
}
