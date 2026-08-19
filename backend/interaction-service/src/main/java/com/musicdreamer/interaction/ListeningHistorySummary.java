package com.musicdreamer.interaction;

import java.time.Instant;

public record ListeningHistorySummary(long trackId, long positionMs, Instant lastListenedAt) {
}
