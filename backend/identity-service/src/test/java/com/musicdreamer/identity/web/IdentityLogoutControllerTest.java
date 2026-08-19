package com.musicdreamer.identity.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;

import com.musicdreamer.identity.application.LogoutApplicationService;
import com.musicdreamer.identity.port.SessionTerminator;
import com.musicdreamer.security.TrustedIdentityHeaders;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class IdentityLogoutControllerTest {

    private RecordingSessionTerminator sessions;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        sessions = new RecordingSessionTerminator();
        mvc = MockMvcBuilders.standaloneSetup(
                        new IdentityLogoutController(new LogoutApplicationService(sessions), false))
                .setControllerAdvice(new IdentityExceptionHandler())
                .build();
    }

    @Test
    void trustedSessionDigestRevokesOnlyThePresentedSession() throws Exception {
        mvc.perform(delete("/api/identity/sessions/current")
                        .header(TrustedIdentityHeaders.SESSION_DIGEST, "trusted-session-digest"))
                .andExpect(status().isNoContent())
                .andExpect(header().string("Set-Cookie", allOf(
                        containsString("music_dreamer_media_session="),
                        containsString("Path=/api/media/objects"),
                        containsString("Max-Age=0"),
                        containsString("HttpOnly"),
                        containsString("SameSite=Strict"))));

        assertThat(sessions.revokedDigest()).isEqualTo("trusted-session-digest");
    }

    @Test
    void missingBearerTokenReturnsStableUnauthorizedProblemDetail() throws Exception {
        mvc.perform(delete("/api/identity/sessions/current")
                        .header("X-Request-Id", "trace-logout-3"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("IDENTITY_SESSION_INVALID"))
                .andExpect(jsonPath("$.traceId").value("trace-logout-3"));
        assertThat(sessions.revokedDigest()).isNull();
    }

    private static final class RecordingSessionTerminator implements SessionTerminator {

        private String revokedDigest;

        @Override
        public void revokeDigest(String tokenDigest) {
            revokedDigest = tokenDigest;
        }

        String revokedDigest() {
            return revokedDigest;
        }
    }
}
