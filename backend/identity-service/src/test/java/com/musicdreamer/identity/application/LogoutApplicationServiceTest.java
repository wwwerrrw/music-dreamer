package com.musicdreamer.identity.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.musicdreamer.identity.port.SessionTerminator;
import java.util.LinkedHashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class LogoutApplicationServiceTest {

    @Test
    void logoutRemovesOnlyPresentedSession() {
        InMemorySessions sessions = new InMemorySessions(
                "current-session-digest", "other-device-session-digest");
        LogoutApplicationService service = new LogoutApplicationService(sessions);

        service.logout("current-session-digest");

        assertThat(sessions.contains("current-session-digest")).isFalse();
        assertThat(sessions.contains("other-device-session-digest")).isTrue();
    }

    private static final class InMemorySessions implements SessionTerminator {

        private final Set<String> tokens = new LinkedHashSet<>();

        private InMemorySessions(String... initialTokens) {
            tokens.addAll(Set.of(initialTokens));
        }

        @Override
        public void revokeDigest(String tokenDigest) {
            tokens.remove(tokenDigest);
        }

        boolean contains(String rawToken) {
            return tokens.contains(rawToken);
        }
    }
}
