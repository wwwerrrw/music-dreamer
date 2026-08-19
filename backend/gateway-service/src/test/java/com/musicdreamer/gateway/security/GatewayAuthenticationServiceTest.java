package com.musicdreamer.gateway.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.musicdreamer.security.AccountStatus;
import com.musicdreamer.security.AuthenticatedPrincipal;
import com.musicdreamer.security.Role;
import com.musicdreamer.security.TrustedIdentityHeaders;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import reactor.core.publisher.Mono;

class GatewayAuthenticationServiceTest {

    @Test
    void requestWithoutResolvedSessionIsRejected() {
        GatewayAuthenticationService service = new GatewayAuthenticationService(
                sessionToken -> Mono.empty(),
                rawToken -> "unused-digest");

        assertThatThrownBy(() -> service.authenticate("missing-session", new HttpHeaders()).block())
                .isInstanceOfSatisfying(GatewayAuthenticationFailure.class, failure ->
                        assertThat(failure.reason())
                                .isEqualTo(GatewayAuthenticationFailure.Reason.SESSION_INVALID));
    }

    @Test
    void resolvedSessionReplacesForgedIdentityBeforeForwarding() {
        GatewayAuthenticationService service = new GatewayAuthenticationService(sessionToken ->
                Mono.just(new AuthenticatedPrincipal(17L, Role.USER, AccountStatus.ACTIVE)),
                rawToken -> "trusted-session-digest");
        HttpHeaders clientHeaders = new HttpHeaders();
        clientHeaders.setBearerAuth("valid-external-token");
        clientHeaders.set(TrustedIdentityHeaders.USER_ID, "999");
        clientHeaders.set(TrustedIdentityHeaders.ROLE, Role.ADMIN.name());

        HttpHeaders trusted = service.authenticate("valid-external-token", clientHeaders).block();

        assertThat(trusted.getFirst(HttpHeaders.AUTHORIZATION)).isNull();
        assertThat(trusted.getFirst(TrustedIdentityHeaders.USER_ID)).isEqualTo("17");
        assertThat(trusted.getFirst(TrustedIdentityHeaders.ROLE)).isEqualTo("USER");
        assertThat(trusted.getFirst(TrustedIdentityHeaders.ACCOUNT_STATUS)).isEqualTo("ACTIVE");
        assertThat(trusted.getFirst(TrustedIdentityHeaders.SESSION_DIGEST))
                .isEqualTo("trusted-session-digest");
    }
}
