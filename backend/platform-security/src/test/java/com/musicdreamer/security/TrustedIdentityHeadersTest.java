package com.musicdreamer.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

class TrustedIdentityHeadersTest {

    @Test
    void replacesForgedIdentityAndDropsExternalSessionCredential() {
        HttpHeaders clientHeaders = new HttpHeaders();
        clientHeaders.setBearerAuth("untrusted-session-token");
        clientHeaders.set(TrustedIdentityHeaders.USER_ID, "999");
        clientHeaders.set(TrustedIdentityHeaders.ROLE, Role.ADMIN.name());
        clientHeaders.set(TrustedIdentityHeaders.ACCOUNT_STATUS, AccountStatus.ACTIVE.name());
        clientHeaders.set("X-Music-Dreamer-Session-Digest", "forged-session-digest");
        clientHeaders.set("X-Request-Id", "request-17");

        HttpHeaders trusted = TrustedIdentityHeaders.forPrincipal(
                clientHeaders,
                new AuthenticatedPrincipal(17L, Role.USER, AccountStatus.ACTIVE),
                "trusted-session-digest");

        assertThat(trusted.getFirst(HttpHeaders.AUTHORIZATION)).isNull();
        assertThat(trusted.getFirst(TrustedIdentityHeaders.USER_ID)).isEqualTo("17");
        assertThat(trusted.getFirst(TrustedIdentityHeaders.ROLE)).isEqualTo("USER");
        assertThat(trusted.getFirst(TrustedIdentityHeaders.ACCOUNT_STATUS)).isEqualTo("ACTIVE");
        assertThat(trusted.getFirst(TrustedIdentityHeaders.SESSION_DIGEST))
                .isEqualTo("trusted-session-digest");
        assertThat(trusted.getFirst("X-Request-Id")).isEqualTo("request-17");
        assertThat(clientHeaders.getFirst(TrustedIdentityHeaders.USER_ID)).isEqualTo("999");
    }
}
