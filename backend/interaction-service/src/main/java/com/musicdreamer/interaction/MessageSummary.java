package com.musicdreamer.interaction;

import java.time.Instant;

public record MessageSummary(
        long id,
        Long senderUserId,
        String audienceType,
        String subject,
        String body,
        Instant publishedAt,
        Instant readAt) {
}
