package com.musicdreamer.catalog;

import java.time.Instant;

public record TrackSummary(
        long id,
        long artistUserId,
        String title,
        long mediaObjectId,
        long durationMs,
        TrackStatus status,
        long playCount,
        Instant createdAt,
        Long coverMediaObjectId,
        Long lyricsMediaObjectId,
        String artistStageName) {

    public TrackSummary(
            long id,
            long artistUserId,
            String title,
            long mediaObjectId,
            long durationMs,
            TrackStatus status,
            long playCount,
            Instant createdAt) {
        this(id, artistUserId, title, mediaObjectId, durationMs, status, playCount, createdAt,
                null, null, null);
    }
}
