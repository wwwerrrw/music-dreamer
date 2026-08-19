package com.musicdreamer.identity.port;

import java.time.Instant;

public interface DevelopmentInbox {

    void deliver(String email, String rawToken, Instant expiresAt);
}
