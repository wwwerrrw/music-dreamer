package com.musicdreamer.security;

public record AuthenticatedPrincipal(long userId, Role role, AccountStatus status) {

    public boolean hasActiveRole(Role requiredRole) {
        return status == AccountStatus.ACTIVE && role == requiredRole;
    }
}
