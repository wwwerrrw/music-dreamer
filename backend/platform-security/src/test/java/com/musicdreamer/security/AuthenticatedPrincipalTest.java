package com.musicdreamer.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AuthenticatedPrincipalTest {

    @Test
    void activePrincipalHasOnlyItsAssignedRole() {
        AuthenticatedPrincipal principal = new AuthenticatedPrincipal(
                42L,
                Role.ADMIN,
                AccountStatus.ACTIVE);

        assertThat(principal.hasActiveRole(Role.ADMIN)).isTrue();
        assertThat(principal.hasActiveRole(Role.SINGER)).isFalse();
    }

    @Test
    void lockedPrincipalHasNoEffectiveRole() {
        AuthenticatedPrincipal principal = new AuthenticatedPrincipal(
                42L,
                Role.ADMIN,
                AccountStatus.LOCKED);

        assertThat(principal.hasActiveRole(Role.ADMIN)).isFalse();
    }
}
