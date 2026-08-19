package com.musicdreamer.identity.inbox;

import com.musicdreamer.identity.port.DevelopmentInbox;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class InMemoryDevelopmentInbox implements DevelopmentInbox {

    private final List<DevelopmentInboxMessage> messages = new CopyOnWriteArrayList<>();

    @Override
    public void deliver(String email, String rawToken, Instant expiresAt) {
        messages.add(new DevelopmentInboxMessage(email, rawToken, expiresAt));
    }

    public List<DevelopmentInboxMessage> messages() {
        return List.copyOf(messages);
    }
}
