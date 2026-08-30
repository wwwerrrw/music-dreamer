package com.musicdreamer.identity.web;

import com.musicdreamer.identity.application.AccountAdministrationService;
import com.musicdreamer.identity.application.IdentityFailure;
import com.musicdreamer.identity.application.UserSummary;
import com.musicdreamer.security.AuthenticatedPrincipal;
import com.musicdreamer.security.TrustedIdentityHeaders;
import java.util.List;
import java.util.Objects;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/identity/admin/users")
public final class IdentityAdminController {

    private final AccountAdministrationService administration;

    public IdentityAdminController(AccountAdministrationService administration) {
        this.administration = Objects.requireNonNull(administration, "administration");
    }

    @GetMapping
    public List<UserSummary> listUsers(@RequestHeader HttpHeaders headers) {
        return administration.listUsers(principal(headers));
    }

    @PostMapping("/{userId}/lock")
    public ResponseEntity<Void> lockUser(
            @RequestHeader HttpHeaders headers,
            @PathVariable long userId) {
        administration.lockUser(principal(headers), userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{userId}/restore")
    public ResponseEntity<Void> restoreUser(
            @RequestHeader HttpHeaders headers,
            @PathVariable long userId) {
        administration.restoreUser(principal(headers), userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{userId}/promote-singer")
    public ResponseEntity<Void> promoteToSinger(
            @RequestHeader HttpHeaders headers,
            @PathVariable long userId) {
        administration.promoteToSinger(principal(headers), userId);
        return ResponseEntity.noContent().build();
    }

    private static AuthenticatedPrincipal principal(HttpHeaders headers) {
        return TrustedIdentityHeaders.principalFrom(headers).orElseThrow(() ->
                new IdentityFailure(IdentityFailure.Reason.FORBIDDEN));
    }
}
