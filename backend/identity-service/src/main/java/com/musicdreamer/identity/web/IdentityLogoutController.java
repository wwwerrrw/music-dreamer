package com.musicdreamer.identity.web;

import com.musicdreamer.identity.application.IdentityFailure;
import com.musicdreamer.identity.application.LogoutApplicationService;
import com.musicdreamer.security.TrustedIdentityHeaders;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/identity/sessions")
public final class IdentityLogoutController {

    private final LogoutApplicationService logout;
    private final boolean mediaCookieSecure;

    public IdentityLogoutController(
            LogoutApplicationService logout,
            @Value("${music-dreamer.identity.media-cookie-secure:false}") boolean mediaCookieSecure) {
        this.logout = Objects.requireNonNull(logout, "logout");
        this.mediaCookieSecure = mediaCookieSecure;
    }

    @DeleteMapping("/current")
    public ResponseEntity<Void> logout(
            @RequestHeader HttpHeaders headers) {
        String tokenDigest = headers.getFirst(TrustedIdentityHeaders.SESSION_DIGEST);
        if (tokenDigest == null || tokenDigest.isBlank()) {
            throw new IdentityFailure(IdentityFailure.Reason.SESSION_INVALID);
        }
        logout.logout(tokenDigest);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, MediaSessionCookie.expire(mediaCookieSecure).toString())
                .build();
    }
}
