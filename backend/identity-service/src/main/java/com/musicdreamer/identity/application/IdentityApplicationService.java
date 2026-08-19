package com.musicdreamer.identity.application;

import com.musicdreamer.identity.port.PasswordVerifier;
import com.musicdreamer.identity.port.SessionIssuer;
import com.musicdreamer.identity.port.UserAccounts;
import com.musicdreamer.security.AccountStatus;
import java.time.Clock;
import java.util.Objects;

public final class IdentityApplicationService {

    private final UserAccounts users;
    private final PasswordVerifier passwordVerifier;
    private final SessionIssuer sessions;
    private final Clock clock;

    public IdentityApplicationService(
            UserAccounts users,
            PasswordVerifier passwordVerifier,
            SessionIssuer sessions,
            Clock clock) {
        this.users = Objects.requireNonNull(users, "users");
        this.passwordVerifier = Objects.requireNonNull(passwordVerifier, "passwordVerifier");
        this.sessions = Objects.requireNonNull(sessions, "sessions");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public LoginSession login(String login, String rawPassword) {
        var account = users.findByLogin(login).orElseThrow(() ->
                new IdentityFailure(IdentityFailure.Reason.INVALID_CREDENTIALS));
        if (!passwordVerifier.matches(rawPassword, account.passwordHash())) {
            throw new IdentityFailure(IdentityFailure.Reason.INVALID_CREDENTIALS);
        }
        if (account.status() == AccountStatus.LOCKED) {
            throw new IdentityFailure(IdentityFailure.Reason.ACCOUNT_LOCKED);
        }
        if (account.status() != AccountStatus.ACTIVE) {
            throw new IdentityFailure(IdentityFailure.Reason.ACCOUNT_NOT_ACTIVE);
        }
        return sessions.issue(account, clock.instant());
    }
}
