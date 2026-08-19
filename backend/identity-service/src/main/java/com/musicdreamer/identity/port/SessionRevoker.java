package com.musicdreamer.identity.port;

public interface SessionRevoker {

    void revokeAllForUser(long userId);
}
