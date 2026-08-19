package com.musicdreamer.identity.port;

@FunctionalInterface
public interface PasswordHasher {

    String hash(String rawPassword);
}
