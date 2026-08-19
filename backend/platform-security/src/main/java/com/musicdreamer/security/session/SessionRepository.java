package com.musicdreamer.security.session;

import com.musicdreamer.security.AuthenticatedPrincipal;
import java.time.Duration;
import java.util.Optional;

public interface SessionRepository {

    void save(String tokenDigest, AuthenticatedPrincipal principal, Duration ttl);

    Optional<AuthenticatedPrincipal> find(String tokenDigest);

    void delete(String tokenDigest);

    void deleteAllForUser(long userId);
}
