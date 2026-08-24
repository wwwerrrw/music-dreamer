package com.musicdreamer.identity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.musicdreamer.identity.domain.UserAccount;
import com.musicdreamer.identity.port.SessionIssuer;
import com.musicdreamer.identity.port.UserAccounts;
import com.musicdreamer.security.AccountStatus;
import com.musicdreamer.security.AuthenticatedPrincipal;
import com.musicdreamer.security.Role;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class IdentityApplicationServiceTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-08-11T10:00:00Z"), ZoneOffset.UTC);

    @Test
    void pendingUserCannotLoginAndNoSessionIsCreated() {
        InMemoryUserAccounts users = new InMemoryUserAccounts();
        users.save(new UserAccount(
                11L,
                "listener",
                "listener@example.test",
                "correct-password-hash",
                Role.USER,
                AccountStatus.PENDING,
                0L));
        RecordingSessionIssuer sessions = new RecordingSessionIssuer();
        IdentityApplicationService service = new IdentityApplicationService(
                users,
                (rawPassword, passwordHash) -> rawPassword.equals("correct-password")
                        && passwordHash.equals("correct-password-hash"),
                sessions,
                CLOCK);

        assertThatThrownBy(() -> service.login("listener", "correct-password"))
                .isInstanceOfSatisfying(IdentityFailure.class, failure ->
                        assertThat(failure.reason()).isEqualTo(IdentityFailure.Reason.ACCOUNT_NOT_ACTIVE));
        assertThat(sessions.issuedCount()).isZero();
    }

    @Test
    void passwordMustMatchBeforeSessionIsIssued() {
        InMemoryUserAccounts users = new InMemoryUserAccounts();
        users.save(new UserAccount(
                12L,
                "active-listener",
                "active-listener@example.test",
                "correct-password-hash",
                Role.USER,
                AccountStatus.ACTIVE,
                0L));
        RecordingSessionIssuer sessions = new RecordingSessionIssuer();
        IdentityApplicationService service = new IdentityApplicationService(
                users,
                (rawPassword, passwordHash) -> rawPassword.equals("correct-password")
                        && passwordHash.equals("correct-password-hash"),
                sessions,
                CLOCK);

        assertThatThrownBy(() -> service.login("active-listener", "wrong-password"))
                .isInstanceOfSatisfying(IdentityFailure.class, failure ->
                        assertThat(failure.reason()).isEqualTo(IdentityFailure.Reason.INVALID_CREDENTIALS));
        assertThat(sessions.issuedCount()).isZero();

        LoginSession session = service.login("active-listener@example.test", "correct-password");
        assertThat(session.token()).isEqualTo("session-token");
        assertThat(session.expiresAt()).isEqualTo(CLOCK.instant().plusSeconds(3600));
        assertThat(sessions.issuedCount()).isEqualTo(1);
    }

    @Test
    void unknownLoginUsesTheSameInvalidCredentialsFailureAndCreatesNoSession() {
        RecordingSessionIssuer sessions = new RecordingSessionIssuer();
        IdentityApplicationService service = new IdentityApplicationService(
                new InMemoryUserAccounts(),
                (rawPassword, passwordHash) -> true,
                sessions,
                CLOCK);

        assertThatThrownBy(() -> service.login("missing-user", "any-password"))
                .isInstanceOfSatisfying(IdentityFailure.class, failure ->
                        assertThat(failure.reason()).isEqualTo(IdentityFailure.Reason.INVALID_CREDENTIALS));
        assertThat(sessions.issuedCount()).isZero();
    }

    @Test
    void activeUserReadsOnlyTheirOwnProfileWithoutPasswordMaterial() {
        InMemoryUserAccounts users = new InMemoryUserAccounts();
        users.save(new UserAccount(
                15L,
                "listener",
                "listener@example.test",
                "must-not-be-exposed",
                Role.USER,
                AccountStatus.ACTIVE,
                2L));
        IdentityApplicationService service = new IdentityApplicationService(
                users,
                (rawPassword, passwordHash) -> false,
                new RecordingSessionIssuer(),
                CLOCK);

        assertThat(service.currentUser(new AuthenticatedPrincipal(15L, Role.USER, AccountStatus.ACTIVE)))
                .isEqualTo(new UserSummary(
                        15L, "listener", "listener@example.test", Role.USER, AccountStatus.ACTIVE, 2L));
        assertThatThrownBy(() -> service.currentUser(
                new AuthenticatedPrincipal(15L, Role.USER, AccountStatus.LOCKED)))
                .isInstanceOfSatisfying(IdentityFailure.class, failure ->
                        assertThat(failure.reason()).isEqualTo(IdentityFailure.Reason.FORBIDDEN));
    }

    private static final class InMemoryUserAccounts implements UserAccounts {

        private final Map<String, UserAccount> usersByLogin = new HashMap<>();

        @Override
        public void save(UserAccount account) {
            usersByLogin.put(account.username(), account);
            usersByLogin.put(account.email(), account);
        }

        @Override
        public Optional<UserAccount> findByLogin(String login) {
            return Optional.ofNullable(usersByLogin.get(login));
        }

        @Override
        public Optional<UserAccount> findById(long userId) {
            return usersByLogin.values().stream()
                    .filter(user -> user.id() == userId)
                    .findFirst();
        }

        @Override
        public List<UserAccount> findAll() {
            return usersByLogin.values().stream()
                    .distinct()
                    .sorted((left, right) -> Long.compare(left.id(), right.id()))
                    .toList();
        }

        @Override
        public UserAccount createPending(String username, String email, String passwordHash) {
            throw new UnsupportedOperationException("not used by this test");
        }
    }

    private static final class RecordingSessionIssuer implements SessionIssuer {

        private int issuedCount;

        @Override
        public LoginSession issue(UserAccount account, Instant issuedAt) {
            issuedCount++;
            return new LoginSession(
                    "session-token",
                    issuedAt.plusSeconds(3600),
                    account.id(),
                    account.role(),
                    account.status());
        }

        int issuedCount() {
            return issuedCount;
        }
    }
}
