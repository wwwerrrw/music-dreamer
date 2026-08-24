package com.musicdreamer.identity.web;

import com.musicdreamer.identity.application.IdentityApplicationService;
import com.musicdreamer.identity.application.IdentityFailure;
import com.musicdreamer.identity.application.UserSummary;
import com.musicdreamer.security.TrustedIdentityHeaders;
import java.util.Objects;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/identity/users")
public final class IdentityProfileController {

    private final IdentityApplicationService identity;

    public IdentityProfileController(IdentityApplicationService identity) {
        this.identity = Objects.requireNonNull(identity, "identity");
    }

    @GetMapping("/me")
    public UserSummary currentUser(@RequestHeader HttpHeaders headers) {
        var principal = TrustedIdentityHeaders.principalFrom(headers).orElseThrow(() ->
                new IdentityFailure(IdentityFailure.Reason.FORBIDDEN));
        return identity.currentUser(principal);
    }
}
