package com.musicdreamer.gateway.security;

import com.musicdreamer.security.AccountStatus;
import com.musicdreamer.security.AuthenticatedPrincipal;
import com.musicdreamer.security.Role;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import reactor.core.publisher.Mono;

public final class PersistentSessionResolver implements SessionResolver {

    private static final String USER_ID = "userId";
    private static final String ROLE = "role";
    private static final String ACCOUNT_STATUS = "accountStatus";

    private final ReactiveStringRedisTemplate redis;
    private final String namespace;
    private final Function<String, String> tokenDigester;

    public PersistentSessionResolver(
            ReactiveStringRedisTemplate redis,
            String namespace,
            Function<String, String> tokenDigester) {
        this.redis = Objects.requireNonNull(redis, "redis");
        this.namespace = Objects.requireNonNull(namespace, "namespace");
        this.tokenDigester = Objects.requireNonNull(tokenDigester, "tokenDigester");
    }

    @Override
    public Mono<AuthenticatedPrincipal> resolve(String sessionToken) {
        return Mono.defer(() -> redis.<String, String>opsForHash()
                        .entries(sessionKey(tokenDigester.apply(sessionToken)))
                        .collectMap(Map.Entry::getKey, Map.Entry::getValue))
                .flatMap(PersistentSessionResolver::toPrincipal);
    }

    private String sessionKey(String tokenDigest) {
        return namespace + ":session:" + tokenDigest;
    }

    private static Mono<AuthenticatedPrincipal> toPrincipal(Map<String, String> fields) {
        if (fields.isEmpty()) {
            return Mono.empty();
        }
        try {
            return Mono.just(new AuthenticatedPrincipal(
                    Long.parseLong(fields.get(USER_ID)),
                    Role.valueOf(fields.get(ROLE)),
                    AccountStatus.valueOf(fields.get(ACCOUNT_STATUS))));
        } catch (IllegalArgumentException | NullPointerException malformedSession) {
            return Mono.empty();
        }
    }
}
