package com.musicdreamer.identity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.musicdreamer.identity.domain.ActivationToken;
import com.musicdreamer.identity.domain.UserAccount;
import com.musicdreamer.identity.port.ActivationTokens;
import com.musicdreamer.identity.port.DevelopmentInbox;
import com.musicdreamer.identity.port.UserAccounts;
import com.musicdreamer.security.AccountStatus;
import com.musicdreamer.security.Role;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class RegistrationApplicationServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-11T10:00:00Z");

    @Test
    void registrationPersistsOnlyDigestsAndDeliversRawActivationToDevelopmentInbox() {
        InMemoryUserAccounts users = new InMemoryUserAccounts();
        InMemoryActivationTokens tokens = new InMemoryActivationTokens();
        RecordingDevelopmentInbox inbox = new RecordingDevelopmentInbox();
        RegistrationApplicationService service = new RegistrationApplicationService(
                users,
                tokens,
                rawPassword -> "$2a$10$fixed-bcrypt-test-hash",
                () -> "raw-activation-token",
                rawToken -> "fixed-sha256-test-digest",
                inbox,
                Clock.fixed(NOW, ZoneOffset.UTC),
                Duration.ofMinutes(30));

        RegistrationResult result = service.register(new RegistrationCommand(
                "new-user",
                "new-user@example.test",
                "clear-text-password"));

        UserAccount user = users.findById(result.userId()).orElseThrow();
        assertThat(user.status()).isEqualTo(AccountStatus.PENDING);
        assertThat(user.role()).isEqualTo(Role.USER);
        assertThat(user.passwordHash()).isEqualTo("$2a$10$fixed-bcrypt-test-hash");
        assertThat(user.passwordHash()).doesNotContain("clear-text-password");
        ActivationToken token = tokens.findByDigest("fixed-sha256-test-digest").orElseThrow();
        assertThat(token.userId()).isEqualTo(user.id());
        assertThat(token.digest()).doesNotContain("raw-activation-token");
        assertThat(token.expiresAt()).isEqualTo(NOW.plus(Duration.ofMinutes(30)));
        assertThat(inbox.deliveries()).containsExactly(new ActivationDelivery(
                "new-user@example.test",
                "raw-activation-token",
                NOW.plus(Duration.ofMinutes(30))));
    }

    @ParameterizedTest
    @CsvSource({
        "existing-user,new-email@example.test",
        "new-user,existing@example.test"
    })
    void existingUsernameOrEmailRejectsRegistrationBeforeActivationIsCreated(
            String requestedUsername,
            String requestedEmail) {
        InMemoryUserAccounts users = new InMemoryUserAccounts();
        users.save(new UserAccount(
                41L,
                "existing-user",
                "existing@example.test",
                "$2a$10$existing-password-hash",
                Role.USER,
                AccountStatus.ACTIVE,
                0L));
        InMemoryActivationTokens tokens = new InMemoryActivationTokens();
        RecordingDevelopmentInbox inbox = new RecordingDevelopmentInbox();
        RegistrationApplicationService service = new RegistrationApplicationService(
                users,
                tokens,
                rawPassword -> "$2a$10$new-password-hash",
                () -> "must-not-be-issued",
                rawToken -> "must-not-be-digested",
                inbox,
                Clock.fixed(NOW, ZoneOffset.UTC),
                Duration.ofMinutes(30));

        assertThatThrownBy(() -> service.register(new RegistrationCommand(
                        requestedUsername,
                        requestedEmail,
                        "new-clear-text-password")))
                .isInstanceOfSatisfying(IdentityFailure.class, failure ->
                        assertThat(failure.reason().name()).isEqualTo("REGISTRATION_CONFLICT"));
        assertThat(tokens.isEmpty()).isTrue();
        assertThat(inbox.deliveries()).isEmpty();
    }

    private static final class InMemoryUserAccounts implements UserAccounts {

        private final Map<Long, UserAccount> users = new LinkedHashMap<>();
        private long nextId = 51L;

        @Override
        public Optional<UserAccount> findByLogin(String login) {
            return users.values().stream()
                    .filter(user -> user.username().equals(login) || user.email().equals(login))
                    .findFirst();
        }

        @Override
        public Optional<UserAccount> findById(long userId) {
            return Optional.ofNullable(users.get(userId));
        }

        @Override
        public List<UserAccount> findAll() {
            return List.copyOf(users.values());
        }

        @Override
        public void save(UserAccount account) {
            users.put(account.id(), account);
        }

        @Override
        public UserAccount createPending(String username, String email, String passwordHash) {
            UserAccount account = new UserAccount(
                    nextId++, username, email, passwordHash, Role.USER, AccountStatus.PENDING, 0L);
            save(account);
            return account;
        }
    }

    private static final class InMemoryActivationTokens implements ActivationTokens {

        private final Map<String, ActivationToken> tokens = new LinkedHashMap<>();
        private long nextId = 61L;

        @Override
        public Optional<ActivationToken> findByDigest(String digest) {
            return Optional.ofNullable(tokens.get(digest));
        }

        @Override
        public void save(ActivationToken token) {
            tokens.put(token.digest(), token);
        }

        @Override
        public ActivationToken create(long userId, String digest, Instant expiresAt) {
            ActivationToken token = new ActivationToken(nextId++, userId, digest, expiresAt, null, 0L);
            save(token);
            return token;
        }

        boolean isEmpty() {
            return tokens.isEmpty();
        }
    }

    private static final class RecordingDevelopmentInbox implements DevelopmentInbox {

        private final List<ActivationDelivery> deliveries = new ArrayList<>();

        @Override
        public void deliver(String email, String rawToken, Instant expiresAt) {
            deliveries.add(new ActivationDelivery(email, rawToken, expiresAt));
        }

        List<ActivationDelivery> deliveries() {
            return List.copyOf(deliveries);
        }
    }

    private record ActivationDelivery(String email, String rawToken, Instant expiresAt) {
    }
}
