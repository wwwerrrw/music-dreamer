package com.musicdreamer.identity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.musicdreamer.identity.domain.ActivationToken;
import com.musicdreamer.identity.domain.UserAccount;
import com.musicdreamer.identity.port.ActivationTokens;
import com.musicdreamer.identity.port.UserAccounts;
import com.musicdreamer.security.AccountStatus;
import com.musicdreamer.security.Role;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ActivationApplicationServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-11T10:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @Test
    void unknownActivationTokenReturnsStableIdentityFailure() {
        ActivationApplicationService service = new ActivationApplicationService(
                new InMemoryUserAccounts(),
                new InMemoryActivationTokens(),
                rawToken -> "digest-of-" + rawToken,
                CLOCK);

        assertThatThrownBy(() -> service.activate("unknown-token"))
                .isInstanceOfSatisfying(IdentityFailure.class, failure ->
                        assertThat(failure.reason())
                                .isEqualTo(IdentityFailure.Reason.ACTIVATION_TOKEN_INVALID));
    }

    @Test
    void expiredActivationTokenChangesNeitherUserNorToken() {
        InMemoryUserAccounts users = new InMemoryUserAccounts();
        users.save(new UserAccount(
                21L,
                "pending-user",
                "pending@example.test",
                "password-hash",
                Role.USER,
                AccountStatus.PENDING,
                0L));
        InMemoryActivationTokens tokens = new InMemoryActivationTokens();
        tokens.save(new ActivationToken(
                31L,
                21L,
                "digest-of-expired-token",
                NOW.minusSeconds(1),
                null,
                0L));
        ActivationApplicationService service = new ActivationApplicationService(
                users,
                tokens,
                rawToken -> "digest-of-" + rawToken,
                CLOCK);

        assertThatThrownBy(() -> service.activate("expired-token"))
                .isInstanceOfSatisfying(IdentityFailure.class, failure ->
                        assertThat(failure.reason()).isEqualTo(IdentityFailure.Reason.ACTIVATION_TOKEN_EXPIRED));
        assertThat(users.findById(21L).orElseThrow().status()).isEqualTo(AccountStatus.PENDING);
        assertThat(tokens.findByDigest("digest-of-expired-token").orElseThrow().usedAt()).isNull();
    }

    @Test
    void activationTokenCanBeUsedOnlyOnce() {
        InMemoryUserAccounts users = new InMemoryUserAccounts();
        users.save(new UserAccount(
                22L,
                "new-listener",
                "new-listener@example.test",
                "password-hash",
                Role.USER,
                AccountStatus.PENDING,
                0L));
        InMemoryActivationTokens tokens = new InMemoryActivationTokens();
        tokens.save(new ActivationToken(
                32L,
                22L,
                "digest-of-valid-token",
                NOW.plusSeconds(600),
                null,
                0L));
        ActivationApplicationService service = new ActivationApplicationService(
                users,
                tokens,
                rawToken -> "digest-of-" + rawToken,
                CLOCK);

        service.activate("valid-token");

        assertThat(users.findById(22L).orElseThrow().status()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(users.findById(22L).orElseThrow().version()).isEqualTo(1L);
        assertThat(tokens.findByDigest("digest-of-valid-token").orElseThrow().usedAt()).isEqualTo(NOW);
        assertThat(tokens.findByDigest("digest-of-valid-token").orElseThrow().version()).isEqualTo(1L);

        assertThatThrownBy(() -> service.activate("valid-token"))
                .isInstanceOfSatisfying(IdentityFailure.class, failure ->
                        assertThat(failure.reason()).isEqualTo(IdentityFailure.Reason.ACTIVATION_TOKEN_USED));
        assertThat(users.findById(22L).orElseThrow().version()).isEqualTo(1L);
        assertThat(tokens.findByDigest("digest-of-valid-token").orElseThrow().version()).isEqualTo(1L);
    }

    private static final class InMemoryUserAccounts implements UserAccounts {

        private final Map<Long, UserAccount> usersById = new HashMap<>();

        @Override
        public Optional<UserAccount> findByLogin(String login) {
            return usersById.values().stream()
                    .filter(user -> user.username().equals(login) || user.email().equals(login))
                    .findFirst();
        }

        @Override
        public Optional<UserAccount> findById(long userId) {
            return Optional.ofNullable(usersById.get(userId));
        }

        @Override
        public List<UserAccount> findAll() {
            return usersById.values().stream()
                    .sorted((left, right) -> Long.compare(left.id(), right.id()))
                    .toList();
        }

        @Override
        public UserAccount createPending(String username, String email, String passwordHash) {
            throw new UnsupportedOperationException("not used by this test");
        }

        @Override
        public void save(UserAccount account) {
            usersById.put(account.id(), account);
        }
    }

    private static final class InMemoryActivationTokens implements ActivationTokens {

        private final Map<String, ActivationToken> tokensByDigest = new HashMap<>();

        @Override
        public Optional<ActivationToken> findByDigest(String digest) {
            return Optional.ofNullable(tokensByDigest.get(digest));
        }

        @Override
        public void save(ActivationToken token) {
            tokensByDigest.put(token.digest(), token);
        }

        @Override
        public ActivationToken create(long userId, String digest, Instant expiresAt) {
            throw new UnsupportedOperationException("not used by this test");
        }
    }
}
