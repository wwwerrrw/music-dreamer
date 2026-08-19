package com.musicdreamer.identity.application;

import com.musicdreamer.identity.port.ActivationTokens;
import com.musicdreamer.identity.port.DevelopmentInbox;
import com.musicdreamer.identity.port.PasswordHasher;
import com.musicdreamer.identity.port.RawTokenGenerator;
import com.musicdreamer.identity.port.TokenDigester;
import com.musicdreamer.identity.port.UserAccounts;
import java.time.Clock;
import java.time.Duration;
import java.util.Objects;
import org.springframework.transaction.annotation.Transactional;

public class RegistrationApplicationService {

    private final UserAccounts users;
    private final ActivationTokens tokens;
    private final PasswordHasher passwordHasher;
    private final RawTokenGenerator rawTokenGenerator;
    private final TokenDigester tokenDigester;
    private final DevelopmentInbox inbox;
    private final Clock clock;
    private final Duration activationLifetime;

    public RegistrationApplicationService(
            UserAccounts users,
            ActivationTokens tokens,
            PasswordHasher passwordHasher,
            RawTokenGenerator rawTokenGenerator,
            TokenDigester tokenDigester,
            DevelopmentInbox inbox,
            Clock clock,
            Duration activationLifetime) {
        this.users = Objects.requireNonNull(users, "users");
        Objects.requireNonNull(tokens, "tokens");
        Objects.requireNonNull(passwordHasher, "passwordHasher");
        Objects.requireNonNull(rawTokenGenerator, "rawTokenGenerator");
        Objects.requireNonNull(tokenDigester, "tokenDigester");
        Objects.requireNonNull(inbox, "inbox");
        Objects.requireNonNull(clock, "clock");
        Objects.requireNonNull(activationLifetime, "activationLifetime");
        this.tokens = tokens;
        this.passwordHasher = passwordHasher;
        this.rawTokenGenerator = rawTokenGenerator;
        this.tokenDigester = tokenDigester;
        this.inbox = inbox;
        this.clock = clock;
        this.activationLifetime = activationLifetime;
    }

    @Transactional
    public RegistrationResult register(RegistrationCommand command) {
        if (users.findByLogin(command.username()).isPresent()
                || users.findByLogin(command.email()).isPresent()) {
            throw new IdentityFailure(IdentityFailure.Reason.REGISTRATION_CONFLICT);
        }
        var user = users.createPending(
                command.username(),
                command.email(),
                passwordHasher.hash(command.rawPassword()));
        var rawToken = rawTokenGenerator.generate();
        var expiresAt = clock.instant().plus(activationLifetime);
        tokens.create(user.id(), tokenDigester.digest(rawToken), expiresAt);
        inbox.deliver(user.email(), rawToken, expiresAt);
        return new RegistrationResult(user.id());
    }
}
