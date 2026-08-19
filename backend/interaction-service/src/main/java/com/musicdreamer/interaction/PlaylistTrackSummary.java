package com.musicdreamer.interaction;

import java.time.Instant;

public record PlaylistTrackSummary(long trackId, int sortOrder, Instant createdAt) {
}
