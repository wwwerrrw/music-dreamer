package com.musicdreamer.identity.application;

import com.musicdreamer.identity.port.SessionTerminator;
import java.util.Objects;

public final class LogoutApplicationService {

    private final SessionTerminator sessions;

    public LogoutApplicationService(SessionTerminator sessions) {
        this.sessions = Objects.requireNonNull(sessions, "sessions");
    }

    public void logout(String tokenDigest) {
        sessions.revokeDigest(tokenDigest);
    }
}
