package com.musicdreamer.identity.port;

@FunctionalInterface
public interface TokenDigester {

    String digest(String rawToken);
}
