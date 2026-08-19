package com.musicdreamer.interaction;

import java.time.Instant;

public record FavoriteSummary(long id, long userId, long trackId, Instant createdAt) {
}
