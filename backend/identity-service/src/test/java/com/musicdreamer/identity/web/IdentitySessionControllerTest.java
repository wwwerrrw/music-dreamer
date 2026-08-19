package com.musicdreamer.identity.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;

import com.musicdreamer.identity.application.IdentityApplicationService;
import com.musicdreamer.identity.application.LoginSession;
import com.musicdreamer.identity.domain.UserAccount;
import com.musicdreamer.identity.port.UserAccounts;
import com.musicdreamer.security.AccountStatus;
import com.musicdreamer.security.Role;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class IdentitySessionControllerTest {

    private static final Instant NOW = Instant.parse("2026-08-11T10:00:00Z");

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        UserAccount account = new UserAccount(
                17L,
                "listener",
                "listener@example.test",
                "stored-password-hash",
                Role.USER,
                AccountStatus.ACTIVE,
                0L);
        IdentityApplicationService service = new IdentityApplicationService(
                new SingleUserAccounts(account),
                (rawPassword, passwordHash) -> rawPassword.equals("correct-password")
                        && passwordHash.equals("stored-password-hash"),
                (authenticatedAccount, issuedAt) ->
                        new LoginSession(
                                "raw-session-token",
                                issuedAt.plusSeconds(3600),
                                authenticatedAccount.id(),
                                authenticatedAccount.role(),
                                authenticatedAccount.status()),
                Clock.fixed(NOW, ZoneOffset.UTC));
        mvc = MockMvcBuilders.standaloneSetup(
                        new IdentitySessionController(service, Clock.fixed(NOW, ZoneOffset.UTC), false))
                .setControllerAdvice(new IdentityExceptionHandler())
                .build();
    }

    @Test
    void correctCredentialsReturnTheIssuedSession() throws Exception {
        mvc.perform(post("/api/identity/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"login":"listener","password":"correct-password"}
                                """))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.token").value("raw-session-token"))
                .andExpect(jsonPath("$.expiresAt").value("2026-08-11T11:00:00Z"))
                .andExpect(jsonPath("$.userId").value(17))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void correctCredentialsSetAStrictMediaOnlySessionCookie() throws Exception {
        mvc.perform(post("/api/identity/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"login":"listener","password":"correct-password"}
                                """))
                .andExpect(status().isOk())
                .andExpect(header().string("Set-Cookie", allOf(
                        containsString("music_dreamer_media_session=raw-session-token"),
                        containsString("Path=/api/media/objects"),
                        containsString("Max-Age=3600"),
                        containsString("HttpOnly"),
                        containsString("SameSite=Strict"))));
    }

    @Test
    void unknownLoginReturnsStableUnauthorizedProblemDetail() throws Exception {
        mvc.perform(post("/api/identity/sessions")
                        .header("X-Request-Id", "trace-http-7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"login":"missing-user","password":"any-password"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("IDENTITY_INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.traceId").value("trace-http-7"));
    }

    private record SingleUserAccounts(UserAccount account) implements UserAccounts {

        @Override
        public Optional<UserAccount> findByLogin(String login) {
            return account.username().equals(login) || account.email().equals(login)
                    ? Optional.of(account)
                    : Optional.empty();
        }

        @Override
        public Optional<UserAccount> findById(long userId) {
            return account.id() == userId ? Optional.of(account) : Optional.empty();
        }

        @Override
        public List<UserAccount> findAll() {
            return List.of(account);
        }

        @Override
        public void save(UserAccount changedAccount) {
            throw new UnsupportedOperationException("not used by this test");
        }

        @Override
        public UserAccount createPending(String username, String email, String passwordHash) {
            throw new UnsupportedOperationException("not used by this test");
        }
    }
}
