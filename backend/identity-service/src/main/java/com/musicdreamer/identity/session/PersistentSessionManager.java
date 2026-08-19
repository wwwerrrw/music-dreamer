package com.musicdreamer.identity.session;

import com.musicdreamer.identity.application.LoginSession;
import com.musicdreamer.identity.domain.UserAccount;
import com.musicdreamer.identity.port.RawTokenGenerator;
import com.musicdreamer.identity.port.SessionIssuer;
import com.musicdreamer.identity.port.SessionRevoker;
import com.musicdreamer.identity.port.SessionTerminator;
import com.musicdreamer.identity.port.TokenDigester;
import com.musicdreamer.security.AuthenticatedPrincipal;
import com.musicdreamer.security.session.SessionRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

public final class PersistentSessionManager implements SessionIssuer, SessionRevoker, SessionTerminator {

    private final SessionRepository repository;
    private final RawTokenGenerator rawTokenGenerator;
    private final TokenDigester tokenDigester;
    private final Duration sessionLifetime;

    public PersistentSessionManager(
            SessionRepository repository,
            RawTokenGenerator rawTokenGenerator,
            TokenDigester tokenDigester,
            Duration sessionLifetime) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.rawTokenGenerator = Objects.requireNonNull(rawTokenGenerator, "rawTokenGenerator");
        this.tokenDigester = Objects.requireNonNull(tokenDigester, "tokenDigester");
        this.sessionLifetime = Objects.requireNonNull(sessionLifetime, "sessionLifetime");
    }

    @Override
    public LoginSession issue(UserAccount account, Instant issuedAt) {
        String rawToken = rawTokenGenerator.generate();
        String tokenDigest = tokenDigester.digest(rawToken);
        repository.save(
                tokenDigest,
                new AuthenticatedPrincipal(account.id(), account.role(), account.status()),
                sessionLifetime);
        return new LoginSession(
                rawToken,
                issuedAt.plus(sessionLifetime),
                account.id(),
                account.role(),
                account.status());
    }

    @Override
    public void revokeDigest(String tokenDigest) {
        repository.delete(tokenDigest);
    }

    @Override
    public void revokeAllForUser(long userId) {
        repository.deleteAllForUser(userId);
    }
}
