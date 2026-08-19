package com.musicdreamer.identity.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.musicdreamer.identity.application.ActivationApplicationService;
import com.musicdreamer.identity.application.RegistrationApplicationService;
import com.musicdreamer.identity.domain.ActivationToken;
import com.musicdreamer.identity.domain.UserAccount;
import com.musicdreamer.identity.inbox.InMemoryDevelopmentInbox;
import com.musicdreamer.identity.port.ActivationTokens;
import com.musicdreamer.identity.port.UserAccounts;
import com.musicdreamer.security.AccountStatus;
import com.musicdreamer.security.Role;
import com.jayway.jsonpath.JsonPath;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class RegistrationActivationHttpFlowTest {

    private static final Instant NOW = Instant.parse("2026-08-11T10:00:00Z");

    private InMemoryUserAccounts users;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        users = new InMemoryUserAccounts();
        InMemoryActivationTokens tokens = new InMemoryActivationTokens();
        InMemoryDevelopmentInbox inbox = new InMemoryDevelopmentInbox();
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        RegistrationApplicationService registration = new RegistrationApplicationService(
                users,
                tokens,
                rawPassword -> "$2a$10$stored-bcrypt-hash",
                () -> "raw-activation-token",
                rawToken -> "sha256-activation-digest",
                inbox,
                clock,
                Duration.ofMinutes(30));
        ActivationApplicationService activation = new ActivationApplicationService(
                users,
                tokens,
                rawToken -> "sha256-activation-digest",
                clock);
        mvc = MockMvcBuilders.standaloneSetup(
                        new IdentityRegistrationController(registration, activation),
                        new DevelopmentInboxController(inbox))
                .setControllerAdvice(new IdentityExceptionHandler())
                .build();
    }

    @Test
    void registrationInboxAndOneTimeActivationCompleteThroughHttp() throws Exception {
        mvc.perform(post("/api/identity/registrations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username":"new-user",
                                  "email":"new-user@example.test",
                                  "password":"clear-text-password"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(51));

        String inboxJson = mvc.perform(get("/api/identity/development-inbox"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].email").value("new-user@example.test"))
                .andExpect(jsonPath("$[0].rawToken").value("raw-activation-token"))
                .andExpect(jsonPath("$[0].expiresAt").value("2026-08-11T10:30:00Z"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String rawToken = JsonPath.read(inboxJson, "$[0].rawToken");

        mvc.perform(post("/api/identity/activations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + rawToken + "\"}"))
                .andExpect(status().isNoContent());
        assertThat(users.findById(51L).orElseThrow().status()).isEqualTo(AccountStatus.ACTIVE);

        mvc.perform(post("/api/identity/activations")
                        .header("X-Request-Id", "trace-activation-9")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + rawToken + "\"}"))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("IDENTITY_ACTIVATION_TOKEN_USED"))
                .andExpect(jsonPath("$.traceId").value("trace-activation-9"));
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
    }
}
