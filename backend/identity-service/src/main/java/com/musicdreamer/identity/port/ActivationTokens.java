package com.musicdreamer.identity.port;

import com.musicdreamer.identity.domain.ActivationToken;
import java.time.Instant;
import java.util.Optional;

public interface ActivationTokens {

    Optional<ActivationToken> findByDigest(String digest);

    void save(ActivationToken token);

    ActivationToken create(long userId, String digest, Instant expiresAt);
}
