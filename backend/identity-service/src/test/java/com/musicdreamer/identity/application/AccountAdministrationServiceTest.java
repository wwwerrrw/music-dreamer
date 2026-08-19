package com.musicdreamer.identity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.musicdreamer.identity.domain.UserAccount;
import com.musicdreamer.identity.port.SessionIssuer;
import com.musicdreamer.identity.port.SessionRevoker;
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

class AccountAdministrationServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-11T10:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @Test
    void activeAdminGetsStableFailureWhenLockTargetDoesNotExist() {
        AccountAdministrationService administration = new AccountAdministrationService(
                new InMemoryUserAccounts(), new InMemorySessions());

        assertThatThrownBy(() -> administration.lockUser(
                new AuthenticatedPrincipal(1L, Role.ADMIN, AccountStatus.ACTIVE),
                404L))
                .isInstanceOfSatisfying(IdentityFailure.class, failure ->
                        assertThat(failure.reason())
                                .isEqualTo(IdentityFailure.Reason.USER_NOT_FOUND));
    }

    @Test
    void lockingUserRevokesExistingSessionsAndRejectsLaterLogin() {
        InMemoryUserAccounts users = new InMemoryUserAccounts();
        users.save(new UserAccount(
                41L,
                "lock-target",
                "lock-target@example.test",
                "password-hash",
                Role.USER,
                AccountStatus.ACTIVE,
                0L));
        InMemorySessions sessions = new InMemorySessions();
        IdentityApplicationService identity = new IdentityApplicationService(
                users,
                (rawPassword, passwordHash) -> rawPassword.equals("correct-password"),
                sessions,
                CLOCK);
        identity.login("lock-target", "correct-password");
        AccountAdministrationService administration = new AccountAdministrationService(users, sessions);

        administration.lockUser(
                new AuthenticatedPrincipal(1L, Role.ADMIN, AccountStatus.ACTIVE),
                41L);

        assertThat(users.findById(41L).orElseThrow().status()).isEqualTo(AccountStatus.LOCKED);
        assertThat(users.findById(41L).orElseThrow().version()).isEqualTo(1L);
        assertThat(sessions.activeCount(41L)).isZero();
        assertThatThrownBy(() -> identity.login("lock-target", "correct-password"))
                .isInstanceOfSatisfying(IdentityFailure.class, failure ->
                        assertThat(failure.reason()).isEqualTo(IdentityFailure.Reason.ACCOUNT_LOCKED));
        assertThat(sessions.totalIssued()).isEqualTo(1);
    }

    @Test
    void regularUserCannotLockAnotherAccount() {
        InMemoryUserAccounts users = new InMemoryUserAccounts();
        users.save(new UserAccount(
                42L,
                "protected-user",
                "protected-user@example.test",
                "password-hash",
                Role.USER,
                AccountStatus.ACTIVE,
                0L));
        AccountAdministrationService administration = new AccountAdministrationService(
                users, new InMemorySessions());

        assertThatThrownBy(() -> administration.lockUser(
                new AuthenticatedPrincipal(2L, Role.USER, AccountStatus.ACTIVE),
                42L))
                .isInstanceOfSatisfying(IdentityFailure.class, failure ->
                        assertThat(failure.reason()).isEqualTo(IdentityFailure.Reason.FORBIDDEN));
        assertThat(users.findById(42L).orElseThrow().status()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(users.findById(42L).orElseThrow().version()).isZero();
    }

    @Test
    void activeAdminCanQueryUsersButRegularUserCannot() {
        InMemoryUserAccounts users = new InMemoryUserAccounts();
        users.save(new UserAccount(
                43L,
                "query-target",
                "query-target@example.test",
                "must-not-be-exposed",
                Role.SINGER,
                AccountStatus.ACTIVE,
                3L));
        AccountAdministrationService administration = new AccountAdministrationService(
                users, new InMemorySessions());

        var result = administration.listUsers(
                new AuthenticatedPrincipal(1L, Role.ADMIN, AccountStatus.ACTIVE));

        assertThat(result).containsExactly(new UserSummary(
                43L,
                "query-target",
                "query-target@example.test",
                Role.SINGER,
                AccountStatus.ACTIVE,
                3L));
        assertThatThrownBy(() -> administration.listUsers(
                new AuthenticatedPrincipal(2L, Role.USER, AccountStatus.ACTIVE)))
                .isInstanceOfSatisfying(IdentityFailure.class, failure ->
                        assertThat(failure.reason()).isEqualTo(IdentityFailure.Reason.FORBIDDEN));
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

    private static final class InMemorySessions implements SessionIssuer, SessionRevoker {

        private final Map<Long, Integer> activeSessions = new HashMap<>();
        private int totalIssued;

        @Override
        public LoginSession issue(UserAccount account, Instant issuedAt) {
            activeSessions.merge(account.id(), 1, Integer::sum);
            totalIssued++;
            return new LoginSession(
                    "session-" + totalIssued,
                    issuedAt.plusSeconds(3600),
                    account.id(),
                    account.role(),
                    account.status());
        }

        @Override
        public void revokeAllForUser(long userId) {
            activeSessions.remove(userId);
        }

        int activeCount(long userId) {
            return activeSessions.getOrDefault(userId, 0);
        }

        int totalIssued() {
            return totalIssued;
        }
    }
}
