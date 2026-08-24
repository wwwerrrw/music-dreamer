package com.musicdreamer.identity.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.musicdreamer.identity.application.AccountAdministrationService;
import com.musicdreamer.identity.domain.UserAccount;
import com.musicdreamer.identity.port.SessionRevoker;
import com.musicdreamer.identity.port.UserAccounts;
import com.musicdreamer.security.AccountStatus;
import com.musicdreamer.security.Role;
import com.musicdreamer.security.TrustedIdentityHeaders;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class IdentityAdminControllerTest {

    private MutableUserAccounts users;
    private RecordingSessionRevoker sessions;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        users = new MutableUserAccounts();
        users.save(new UserAccount(
                17L,
                "listener",
                "listener@example.test",
                "$2a$10$secret-password-hash",
                Role.USER,
                AccountStatus.ACTIVE,
                0L));
        sessions = new RecordingSessionRevoker();
        mvc = MockMvcBuilders.standaloneSetup(new IdentityAdminController(
                        new AccountAdministrationService(users, sessions)))
                .setControllerAdvice(new IdentityExceptionHandler())
                .build();
    }

    @Test
    void activeAdminCanListPasswordFreeSummariesAndLockAUser() throws Exception {
        mvc.perform(get("/api/identity/admin/users")
                        .headers(trustedHeaders(Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(17))
                .andExpect(jsonPath("$[0].username").value("listener"))
                .andExpect(jsonPath("$[0].email").value("listener@example.test"))
                .andExpect(jsonPath("$[0].passwordHash").doesNotExist());

        mvc.perform(post("/api/identity/admin/users/17/lock")
                        .headers(trustedHeaders(Role.ADMIN)))
                .andExpect(status().isNoContent());

        assertThat(users.findById(17L).orElseThrow().status()).isEqualTo(AccountStatus.LOCKED);
        assertThat(sessions.revokedUserId()).isEqualTo(17L);
    }

    @Test
    void ordinaryUserReceivesStableForbiddenProblemDetail() throws Exception {
        mvc.perform(get("/api/identity/admin/users")
                        .header("X-Request-Id", "trace-admin-4")
                        .headers(trustedHeaders(Role.USER)))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("IDENTITY_FORBIDDEN"))
                .andExpect(jsonPath("$.traceId").value("trace-admin-4"));
    }

    @Test
    void activeAdminPromotesARegularUserAndRevokesTheirSessions() throws Exception {
        mvc.perform(post("/api/identity/admin/users/17/promote-singer")
                        .headers(trustedHeaders(Role.ADMIN)))
                .andExpect(status().isNoContent());

        UserAccount promoted = users.findById(17L).orElseThrow();
        assertThat(promoted.role()).isEqualTo(Role.SINGER);
        assertThat(promoted.status()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(promoted.version()).isEqualTo(1L);
        assertThat(sessions.revokedUserId()).isEqualTo(17L);
    }

    @Test
    void promotingAnAccountThatIsNotARegularUserReturnsConflict() throws Exception {
        users.save(new UserAccount(
                18L,
                "existing-singer",
                "existing-singer@example.test",
                "$2a$10$secret-password-hash",
                Role.SINGER,
                AccountStatus.ACTIVE,
                0L));

        mvc.perform(post("/api/identity/admin/users/18/promote-singer")
                        .headers(trustedHeaders(Role.ADMIN)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("IDENTITY_ROLE_CONFLICT"));

        assertThat(users.findById(18L).orElseThrow().version()).isZero();
    }

    private static org.springframework.http.HttpHeaders trustedHeaders(Role role) {
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.set(TrustedIdentityHeaders.USER_ID, "1");
        headers.set(TrustedIdentityHeaders.ROLE, role.name());
        headers.set(TrustedIdentityHeaders.ACCOUNT_STATUS, AccountStatus.ACTIVE.name());
        return headers;
    }

    private static final class MutableUserAccounts implements UserAccounts {

        private final Map<Long, UserAccount> users = new LinkedHashMap<>();

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
            throw new UnsupportedOperationException("not used by this test");
        }
    }

    private static final class RecordingSessionRevoker implements SessionRevoker {

        private Long revokedUserId;

        @Override
        public void revokeAllForUser(long userId) {
            revokedUserId = userId;
        }

        Long revokedUserId() {
            return revokedUserId;
        }
    }
}
