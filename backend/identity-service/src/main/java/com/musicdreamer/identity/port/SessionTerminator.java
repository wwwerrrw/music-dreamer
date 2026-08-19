package com.musicdreamer.identity.port;

public interface SessionTerminator {

    void revokeDigest(String tokenDigest);
}
