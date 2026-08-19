package com.musicdreamer.gateway.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.musicdreamer.security.AccountStatus;
import com.musicdreamer.security.AuthenticatedPrincipal;
import com.musicdreamer.security.Role;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.ReactiveHashOperations;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import reactor.core.publisher.Flux;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PersistentSessionResolverTest {

    @Test
    void presentedTokenIsDigestedBeforeTheStoredPrincipalIsResolved() {
        ReactiveStringRedisTemplate redis = mock(ReactiveStringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ReactiveHashOperations<String, String, String> hashes = mock(ReactiveHashOperations.class);
        when(redis.<String, String>opsForHash()).thenReturn(hashes);
        when(hashes.entries("music-dreamer:test:p1:session:sha256-session-digest"))
                .thenReturn(Flux.just(
                        Map.entry("userId", "17"),
                        Map.entry("role", "USER"),
                        Map.entry("accountStatus", "ACTIVE")));
        when(hashes.entries("music-dreamer:test:p1:session:unknown-digest"))
                .thenReturn(Flux.empty());
        AuthenticatedPrincipal principal =
                new AuthenticatedPrincipal(17L, Role.USER, AccountStatus.ACTIVE);
        PersistentSessionResolver resolver = new PersistentSessionResolver(
                redis,
                "music-dreamer:test:p1",
                rawToken -> rawToken.equals("raw-session-token")
                        ? "sha256-session-digest"
                        : "unknown-digest");

        assertThat(resolver.resolve("raw-session-token").block(Duration.ofSeconds(1)))
                .isEqualTo(principal);
        assertThat(resolver.resolve("missing-session-token").block(Duration.ofSeconds(1)))
                .isNull();
    }

    @Test
    void malformedStoredSessionIsRejectedAsMissing() {
        ReactiveStringRedisTemplate redis = mock(ReactiveStringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ReactiveHashOperations<String, String, String> hashes = mock(ReactiveHashOperations.class);
        when(redis.<String, String>opsForHash()).thenReturn(hashes);
        when(hashes.entries("music-dreamer:test:p1:session:digest"))
                .thenReturn(Flux.just(
                        Map.entry("userId", "17"),
                        Map.entry("role", "SUPERUSER"),
                        Map.entry("accountStatus", "ACTIVE")));
        PersistentSessionResolver resolver = new PersistentSessionResolver(
                redis,
                "music-dreamer:test:p1",
                rawToken -> "digest");

        assertThat(resolver.resolve("raw-session-token").block(Duration.ofSeconds(1)))
                .isNull();
    }
}
