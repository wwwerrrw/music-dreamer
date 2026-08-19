package com.musicdreamer.identity.security;

import com.musicdreamer.identity.port.PasswordHasher;
import com.musicdreamer.identity.port.PasswordVerifier;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public final class BCryptCredentials implements PasswordHasher, PasswordVerifier {

    private final BCryptPasswordEncoder encoder;

    public BCryptCredentials(int strength) {
        this.encoder = new BCryptPasswordEncoder(strength);
    }

    @Override
    public String hash(String rawPassword) {
        return encoder.encode(rawPassword);
    }

    @Override
    public boolean matches(String rawPassword, String passwordHash) {
        return encoder.matches(rawPassword, passwordHash);
    }
}
