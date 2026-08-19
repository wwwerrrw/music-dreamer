package com.musicdreamer.identity.inbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class InMemoryDevelopmentInboxTest {

    @Test
    void deliveriesAreReturnedInOrderAsAnImmutableSnapshot() {
        InMemoryDevelopmentInbox inbox = new InMemoryDevelopmentInbox();
        inbox.deliver(
                "first@example.test",
                "first-raw-token",
                Instant.parse("2026-08-11T10:30:00Z"));
        inbox.deliver(
                "second@example.test",
                "second-raw-token",
                Instant.parse("2026-08-11T10:45:00Z"));

        var snapshot = inbox.messages();

        assertThat(snapshot).containsExactly(
                new DevelopmentInboxMessage(
                        "first@example.test",
                        "first-raw-token",
                        Instant.parse("2026-08-11T10:30:00Z")),
                new DevelopmentInboxMessage(
                        "second@example.test",
                        "second-raw-token",
                        Instant.parse("2026-08-11T10:45:00Z")));
        assertThatThrownBy(() -> snapshot.clear()).isInstanceOf(UnsupportedOperationException.class);
    }
}
