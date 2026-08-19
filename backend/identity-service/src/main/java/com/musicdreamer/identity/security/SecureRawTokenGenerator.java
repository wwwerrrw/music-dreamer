package com.musicdreamer.identity.security;

import com.musicdreamer.identity.port.RawTokenGenerator;
import java.security.SecureRandom;
import java.util.Base64;

public final class SecureRawTokenGenerator implements RawTokenGenerator {

    private static final int TOKEN_BYTES = 32;

    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public String generate() {
        byte[] value = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(value);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }
}
