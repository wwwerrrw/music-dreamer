package com.musicdreamer.identity.session;

import static org.assertj.core.api.Assertions.assertThat;

import com.musicdreamer.identity.domain.UserAccount;
import com.musicdreamer.security.AccountStatus;
import com.musicdreamer.security.AuthenticatedPrincipal;
import com.musicdreamer.security.Role;
import com.musicdreamer.security.session.SessionRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PersistentSessionManagerTest {

    private static final Duration SESSION_LIFETIME = Duration.ofHours(1);
    private static final Instant ISSUED_AT = Instant.parse("2026-08-11T10:00:00Z");

    @Test
    void issueStoresOnlyTheTokenDigestWithPrincipalAndLifetime() {
        InMemorySessionRepository repository = new InMemorySessionRepository();
        PersistentSessionManager sessions = new PersistentSessionManager(
                repository,
                () -> "raw-session-token",
                rawToken -> "sha256-session-digest",
                SESSION_LIFETIME);
        UserAccount account = new UserAccount(
                17L,
                "listener",
                "listener@example.test",
                "$2a$10$stored-bcrypt-hash",
                Role.USER,
                AccountStatus.ACTIVE,
                0L);

        var issued = sessions.issue(account, ISSUED_AT);

        assertThat(issued.token()).isEqualTo("raw-session-token");
        assertThat(issued.expiresAt()).isEqualTo(ISSUED_AT.plus(SESSION_LIFETIME));
        assertThat(repository.find("raw-session-token")).isEmpty();
        assertThat(repository.find("sha256-session-digest"))
                .contains(new AuthenticatedPrincipal(17L, Role.USER, AccountStatus.ACTIVE));
        assertThat(repository.ttl("sha256-session-digest")).contains(SESSION_LIFETIME);
    }

    @Test
    void revokeDigestDeletesOnlyTheTrustedSession() {
        InMemorySessionRepository repository = new InMemorySessionRepository();
        repository.save(
                "sha256-session-digest",
                new AuthenticatedPrincipal(17L, Role.USER, AccountStatus.ACTIVE),
                SESSION_LIFETIME);
        repository.save(
                "other-session-digest",
                new AuthenticatedPrincipal(23L, Role.SINGER, AccountStatus.ACTIVE),
                SESSION_LIFETIME);
        PersistentSessionManager sessions = new PersistentSessionManager(
                repository,
                () -> "unused",
                rawToken -> "unexpected-digest",
                SESSION_LIFETIME);

        sessions.revokeDigest("sha256-session-digest");

        assertThat(repository.find("sha256-session-digest")).isEmpty();
        assertThat(repository.find("other-session-digest")).isPresent();
    }

    @Test
    void revokeAllForUserLeavesOtherUsersSessionsIntact() {
        InMemorySessionRepository repository = new InMemorySessionRepository();
        repository.save(
                "first-user-session",
                new AuthenticatedPrincipal(17L, Role.USER, AccountStatus.ACTIVE),
                SESSION_LIFETIME);
        repository.save(
                "second-user-session",
                new AuthenticatedPrincipal(17L, Role.USER, AccountStatus.ACTIVE),
                SESSION_LIFETIME);
        repository.save(
                "artist-session",
                new AuthenticatedPrincipal(23L, Role.SINGER, AccountStatus.ACTIVE),
                SESSION_LIFETIME);
        PersistentSessionManager sessions = new PersistentSessionManager(
                repository,
                () -> "unused",
                rawToken -> "unused",
                SESSION_LIFETIME);

        sessions.revokeAllForUser(17L);

        assertThat(repository.find("first-user-session")).isEmpty();
        assertThat(repository.find("second-user-session")).isEmpty();
        assertThat(repository.find("artist-session")).isPresent();
    }

    private static final class InMemorySessionRepository implements SessionRepository {

        private final Map<String, StoredSession> sessions = new LinkedHashMap<>();

        @Override
        public void save(String tokenDigest, AuthenticatedPrincipal principal, Duration ttl) {
            sessions.put(tokenDigest, new StoredSession(principal, ttl));
        }

        @Override
        public Optional<AuthenticatedPrincipal> find(String tokenDigest) {
            return Optional.ofNullable(sessions.get(tokenDigest)).map(StoredSession::principal);
        }

        @Override
        public void delete(String tokenDigest) {
            sessions.remove(tokenDigest);
        }

        @Override
        public void deleteAllForUser(long userId) {
            sessions.entrySet().removeIf(entry -> entry.getValue().principal().userId() == userId);
        }

        Optional<Duration> ttl(String tokenDigest) {
            return Optional.ofNullable(sessions.get(tokenDigest)).map(StoredSession::ttl);
        }
    }

    private record StoredSession(AuthenticatedPrincipal principal, Duration ttl) {
    }
}
