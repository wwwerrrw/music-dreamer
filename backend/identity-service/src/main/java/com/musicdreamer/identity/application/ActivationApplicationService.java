package com.musicdreamer.identity.application;

import com.musicdreamer.identity.port.ActivationTokens;
import com.musicdreamer.identity.port.TokenDigester;
import com.musicdreamer.identity.port.UserAccounts;
import java.time.Clock;
import java.util.Objects;
import org.springframework.transaction.annotation.Transactional;

public class ActivationApplicationService {

    private final UserAccounts users;
    private final ActivationTokens tokens;
    private final TokenDigester tokenDigester;
    private final Clock clock;

    public ActivationApplicationService(
            UserAccounts users,
            ActivationTokens tokens,
            TokenDigester tokenDigester,
            Clock clock) {
        this.users = Objects.requireNonNull(users, "users");
        this.tokens = Objects.requireNonNull(tokens, "tokens");
        this.tokenDigester = Objects.requireNonNull(tokenDigester, "tokenDigester");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Transactional
    public void activate(String rawToken) {
        var token = tokens.findByDigest(tokenDigester.digest(rawToken)).orElseThrow(() ->
                new IdentityFailure(IdentityFailure.Reason.ACTIVATION_TOKEN_INVALID));
        var now = clock.instant();
        if (token.usedAt() != null) {
            throw new IdentityFailure(IdentityFailure.Reason.ACTIVATION_TOKEN_USED);
        }
        if (!token.expiresAt().isAfter(now)) {
            throw new IdentityFailure(IdentityFailure.Reason.ACTIVATION_TOKEN_EXPIRED);
        }
        var user = users.findById(token.userId()).orElseThrow(() ->
                new IdentityFailure(IdentityFailure.Reason.USER_NOT_FOUND));
        users.save(user.activate());
        tokens.save(token.markUsed(now));
    }
}
