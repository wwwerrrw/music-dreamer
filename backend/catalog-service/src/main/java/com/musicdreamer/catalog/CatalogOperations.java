package com.musicdreamer.catalog;

import com.musicdreamer.security.Role;
import java.util.List;

public interface CatalogOperations {

    TrackSummary createDraft(long singerUserId, String title, long mediaObjectId, long durationMs, Role role);

    TrackSummary createDraft(
            long singerUserId,
            String title,
            long mediaObjectId,
            long durationMs,
            Long coverMediaObjectId,
            Long lyricsMediaObjectId,
            Role role);

    TrackSummary submit(long singerUserId, long trackId);

    TrackSummary updateDraft(long singerUserId, long trackId, String title, long durationMs);

    TrackSummary updateDraft(
            long singerUserId,
            long trackId,
            String title,
            long durationMs,
            Long coverMediaObjectId,
            Long lyricsMediaObjectId);

    void removePublished(long singerUserId, long trackId);

    TrackSummary publish(long adminUserId, long trackId);

    TrackSummary lock(long adminUserId, long trackId);

    TrackSummary find(long trackId);

    TrackSummary playable(long trackId);

    TrackSummary recordPlay(long trackId);

    List<TrackSummary> search(String keyword, TrackSort sort);

    List<ArtistSummary> searchArtists(String keyword);

    List<TrackSummary> publishedByArtist(long userId);

    TrackPage adminTracks(TrackStatus status, int page, int size);

    ArtistSummary upsertArtist(long singerUserId, String stageName, String biography);

    ArtistSummary findArtist(long userId);
}
