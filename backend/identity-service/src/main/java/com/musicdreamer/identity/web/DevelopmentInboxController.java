package com.musicdreamer.identity.web;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.musicdreamer.identity.inbox.InMemoryDevelopmentInbox;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/identity/development-inbox")
public final class DevelopmentInboxController {

    private final InMemoryDevelopmentInbox inbox;

    public DevelopmentInboxController(InMemoryDevelopmentInbox inbox) {
        this.inbox = Objects.requireNonNull(inbox, "inbox");
    }

    @GetMapping
    public List<DevelopmentInboxResponse> messages() {
        return inbox.messages().stream()
                .map(message -> new DevelopmentInboxResponse(
                        message.email(), message.rawToken(), message.expiresAt()))
                .toList();
    }

    public record DevelopmentInboxResponse(
            String email,
            String rawToken,
            @JsonFormat(shape = JsonFormat.Shape.STRING) Instant expiresAt) {
    }
}
