package com.musicdreamer.identity.web;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.musicdreamer.identity.application.IdentityApplicationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/identity/sessions")
public final class IdentitySessionController {

    private final IdentityApplicationService identity;
    private final Clock clock;
    private final boolean mediaCookieSecure;

    public IdentitySessionController(
            IdentityApplicationService identity,
            Clock clock,
            @Value("${music-dreamer.identity.media-cookie-secure:false}") boolean mediaCookieSecure) {
        this.identity = Objects.requireNonNull(identity, "identity");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.mediaCookieSecure = mediaCookieSecure;
    }

    @PostMapping
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        var session = identity.login(request.login(), request.password());
        Duration maxAge = Duration.ofSeconds(Math.max(
                0L,
                Duration.between(clock.instant(), session.expiresAt()).getSeconds()));
        var mediaCookie = MediaSessionCookie.issue(session.token(), maxAge, mediaCookieSecure);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, mediaCookie.toString())
                .body(new LoginResponse(
                        session.token(),
                        session.expiresAt(),
                        session.userId(),
                        session.role().name(),
                        session.status().name()));
    }

    public record LoginRequest(
            @NotBlank String login,
            @NotBlank String password) {
    }

    public record LoginResponse(
            String token,
            @JsonFormat(shape = JsonFormat.Shape.STRING) Instant expiresAt,
            long userId,
            String role,
            String status) {
    }
}
