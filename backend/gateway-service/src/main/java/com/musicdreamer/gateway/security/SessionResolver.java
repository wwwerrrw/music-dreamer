package com.musicdreamer.gateway.security;

import com.musicdreamer.security.AuthenticatedPrincipal;
import reactor.core.publisher.Mono;

@FunctionalInterface
public interface SessionResolver {

    Mono<AuthenticatedPrincipal> resolve(String sessionToken);
}
