package com.musicdreamer.identity.security;

import com.musicdreamer.identity.port.TokenDigester;
import com.musicdreamer.security.Sha256Tokens;

public final class Sha256TokenDigester implements TokenDigester {

    @Override
    public String digest(String rawToken) {
        return Sha256Tokens.digest(rawToken);
    }
}
