package com.musicdreamer.security.session;

import com.musicdreamer.security.AccountStatus;
import com.musicdreamer.security.AuthenticatedPrincipal;
import com.musicdreamer.security.Role;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class RedisSessionRepository implements SessionRepository {

    private static final String USER_ID = "userId";
    private static final String ROLE = "role";
    private static final String ACCOUNT_STATUS = "accountStatus";

    private final RedisSessionDataStore dataStore;
    private final String namespace;

    public RedisSessionRepository(RedisSessionDataStore dataStore, String namespace) {
        this.dataStore = Objects.requireNonNull(dataStore, "dataStore");
        this.namespace = Objects.requireNonNull(namespace, "namespace");
    }

    @Override
    public void save(String tokenDigest, AuthenticatedPrincipal principal, Duration ttl) {
        String sessionKey = sessionKey(tokenDigest);
        dataStore.writeHash(
                sessionKey,
                Map.of(
                        USER_ID, Long.toString(principal.userId()),
                        ROLE, principal.role().name(),
                        ACCOUNT_STATUS, principal.status().name()),
                ttl);
        dataStore.addSetMember(userSessionsKey(principal.userId()), sessionKey, ttl);
    }

    @Override
    public Optional<AuthenticatedPrincipal> find(String tokenDigest) {
        Map<String, String> fields = dataStore.readHash(sessionKey(tokenDigest));
        if (fields.isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(new AuthenticatedPrincipal(
                    Long.parseLong(fields.get(USER_ID)),
                    Role.valueOf(fields.get(ROLE)),
                    AccountStatus.valueOf(fields.get(ACCOUNT_STATUS))));
        } catch (IllegalArgumentException | NullPointerException malformedSession) {
            return Optional.empty();
        }
    }

    @Override
    public void delete(String tokenDigest) {
        String sessionKey = sessionKey(tokenDigest);
        Map<String, String> fields = dataStore.readHash(sessionKey);
        dataStore.delete(sessionKey);
        if (fields.containsKey(USER_ID)) {
            dataStore.removeSetMember(
                    userSessionsKey(Long.parseLong(fields.get(USER_ID))),
                    sessionKey);
        }
    }

    @Override
    public void deleteAllForUser(long userId) {
        String userSessionsKey = userSessionsKey(userId);
        dataStore.delete(dataStore.readSet(userSessionsKey));
        dataStore.delete(userSessionsKey);
    }

    private String sessionKey(String tokenDigest) {
        return namespace + ":session:" + tokenDigest;
    }

    private String userSessionsKey(long userId) {
        return namespace + ":user-sessions:" + userId;
    }
}
