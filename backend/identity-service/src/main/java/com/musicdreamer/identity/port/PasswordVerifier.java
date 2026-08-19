package com.musicdreamer.identity.port;

@FunctionalInterface
public interface PasswordVerifier {

    boolean matches(String rawPassword, String passwordHash);
}
