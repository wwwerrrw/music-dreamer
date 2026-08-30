package com.musicdreamer.catalog;

import com.musicdreamer.security.Role;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.transaction.annotation.Transactional;

public class CatalogApplicationService implements CatalogOperations {

    private static final String TRACK_COLUMNS = "t.id, t.artist_user_id, t.title, t.media_object_id, "
            + "t.duration_ms, t.status, t.play_count, t.created_at, t.cover_media_object_id, "
            + "t.lyrics_media_object_id, a.stage_name AS artist_stage_name ";

    private final JdbcTemplate jdbc;

    public CatalogApplicationService(JdbcTemplate jdbc) {
        this.jdbc = Objects.requireNonNull(jdbc, "jdbc");
    }

    public TrackSummary createDraft(long singerUserId, String title, long mediaObjectId, long durationMs) {
        return createDraft(singerUserId, title, mediaObjectId, durationMs, Role.SINGER);
    }

    @Transactional
    public TrackSummary createDraft(
            long singerUserId,
            String title,
            long mediaObjectId,
            long durationMs,
            Role role) {
        requireRole(role, Role.SINGER);
        if (title == null || title.isBlank() || title.length() > 200 || durationMs <= 0) {
            throw new CatalogStateException("Track title and duration are invalid");
        }
        Integer artistCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM artist_profiles WHERE user_id = ?", Integer.class, singerUserId);
        if (artistCount == null || artistCount == 0) {
            jdbc.update("INSERT INTO artist_profiles (user_id, stage_name, status) VALUES (?, ?, ?)",
                    singerUserId, "Singer " + singerUserId, "ACTIVE");
        }
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            var statement = connection.prepareStatement(
                    "INSERT INTO tracks (artist_user_id, title, media_object_id, duration_ms, status) "
                            + "VALUES (?, ?, ?, ?, ?)",
                    new String[] {"id"});
            statement.setLong(1, singerUserId);
            statement.setString(2, title.trim());
            statement.setLong(3, mediaObjectId);
            statement.setLong(4, durationMs);
            statement.setString(5, TrackStatus.DRAFT.name());
            return statement;
        }, keyHolder);
        Number generatedId = keyHolder.getKey();
        if (generatedId == null) {
            throw new IllegalStateException("Track insert did not return an id");
        }
        return find(generatedId.longValue());
    }

    @Override
    @Transactional
    public TrackSummary createDraft(
            long singerUserId,
            String title,
            long mediaObjectId,
            long durationMs,
            Long coverMediaObjectId,
            Long lyricsMediaObjectId,
            Role role) {
        TrackSummary created = createDraft(singerUserId, title, mediaObjectId, durationMs, role);
        jdbc.update(
                "UPDATE tracks SET cover_media_object_id = ?, lyrics_media_object_id = ? WHERE id = ?",
                coverMediaObjectId,
                lyricsMediaObjectId,
                created.id());
        return find(created.id());
    }

    @Transactional
    public TrackSummary submit(long singerUserId, long trackId) {
        TrackSummary track = find(trackId);
        if (track.artistUserId() != singerUserId) {
            throw new CatalogAuthorizationException();
        }
        if (track.status() != TrackStatus.DRAFT) {
            throw new CatalogStateException("Only draft tracks can be submitted");
        }
        jdbc.update("UPDATE tracks SET status = ?, version = version + 1 WHERE id = ? AND status = ?",
                TrackStatus.PENDING_REVIEW.name(), trackId, TrackStatus.DRAFT.name());
        return find(trackId);
    }

    @Transactional
    public TrackSummary updateDraft(long singerUserId, long trackId, String title, long durationMs) {
        validateDraftUpdate(singerUserId, trackId, title, durationMs);
        jdbc.update("UPDATE tracks SET title = ?, duration_ms = ?, version = version + 1 WHERE id = ?",
                title.trim(), durationMs, trackId);
        return find(trackId);
    }

    @Override
    @Transactional
    public TrackSummary updateDraft(
            long singerUserId,
            long trackId,
            String title,
            long durationMs,
            Long coverMediaObjectId,
            Long lyricsMediaObjectId) {
        validateDraftUpdate(singerUserId, trackId, title, durationMs);
        jdbc.update(
                "UPDATE tracks SET title = ?, duration_ms = ?, cover_media_object_id = ?, "
                        + "lyrics_media_object_id = ?, version = version + 1 WHERE id = ?",
                title.trim(),
                durationMs,
                coverMediaObjectId,
                lyricsMediaObjectId,
                trackId);
        return find(trackId);
    }

    private TrackSummary validateDraftUpdate(long singerUserId, long trackId, String title, long durationMs) {
        TrackSummary track = find(trackId);
        if (track.artistUserId() != singerUserId) {
            throw new CatalogAuthorizationException();
        }
        if (track.status() != TrackStatus.DRAFT) {
            throw new CatalogStateException("Only draft tracks can be edited");
        }
        if (title == null || title.isBlank() || title.length() > 200 || durationMs <= 0) {
            throw new CatalogStateException("Track title and duration are invalid");
        }
        return track;
    }

    @Override
    @Transactional
    public void removePublished(long singerUserId, long trackId) {
        TrackSummary track = find(trackId);
        if (track.artistUserId() != singerUserId) {
            throw new CatalogAuthorizationException();
        }
        if (track.status() != TrackStatus.PUBLISHED) {
            throw new CatalogStateException("Only published tracks can be removed");
        }
        int updated = jdbc.update(
                "UPDATE tracks SET status = ?, version = version + 1 "
                        + "WHERE id = ? AND artist_user_id = ? AND status = ?",
                TrackStatus.REMOVED.name(),
                trackId,
                singerUserId,
                TrackStatus.PUBLISHED.name());
        if (updated == 0) {
            throw new CatalogStateException("Track state changed before it could be removed");
        }
    }

    @Transactional
    public TrackSummary publish(long adminUserId, long trackId) {
        TrackSummary track = find(trackId);
        if (track.status() != TrackStatus.PENDING_REVIEW) {
            throw new CatalogStateException("Only submitted tracks can be published");
        }
        jdbc.update("UPDATE tracks SET status = ?, version = version + 1 WHERE id = ?",
                TrackStatus.PUBLISHED.name(), trackId);
        return find(trackId);
    }

    @Transactional
    public TrackSummary lock(long adminUserId, long trackId) {
        find(trackId);
        jdbc.update("UPDATE tracks SET status = ?, version = version + 1 WHERE id = ?",
                TrackStatus.LOCKED.name(), trackId);
        return find(trackId);
    }

    @Override
    @Transactional
    public TrackSummary restore(long adminUserId, long trackId) {
        TrackSummary track = find(trackId);
        if (track.status() != TrackStatus.LOCKED) {
            throw new CatalogStateException("Only locked tracks can be restored");
        }
        int updated = jdbc.update(
                "UPDATE tracks SET status = ?, version = version + 1 WHERE id = ? AND status = ?",
                TrackStatus.PUBLISHED.name(), trackId, TrackStatus.LOCKED.name());
        if (updated == 0) {
            throw new CatalogStateException("Track state changed before it could be restored");
        }
        return find(trackId);
    }

    public TrackSummary find(long trackId) {
        return jdbc.queryForObject(
                "SELECT " + TRACK_COLUMNS
                        + "FROM tracks t LEFT JOIN artist_profiles a ON a.user_id = t.artist_user_id "
                        + "WHERE t.id = ?",
                trackMapper(),
                trackId);
    }

    @Override
    public TrackSummary playable(long trackId) {
        TrackSummary track = find(trackId);
        if (track.status() != TrackStatus.PUBLISHED) {
            throw new CatalogStateException("Track is not available for playback");
        }
        return track;
    }

    @Override
    public List<TrackSummary> playableAll(List<Long> trackIds) {
        Objects.requireNonNull(trackIds, "trackIds");
        if (trackIds.isEmpty()) {
            return List.of();
        }
        String placeholders = String.join(",", Collections.nCopies(trackIds.size(), "?"));
        Object[] args = new Object[trackIds.size() + 1];
        args[0] = TrackStatus.PUBLISHED.name();
        for (int index = 0; index < trackIds.size(); index++) {
            args[index + 1] = trackIds.get(index);
        }
        List<TrackSummary> playable = jdbc.query(
                "SELECT " + TRACK_COLUMNS
                        + "FROM tracks t LEFT JOIN artist_profiles a ON a.user_id = t.artist_user_id "
                        + "WHERE t.status = ? AND t.id IN (" + placeholders + ")",
                trackMapper(),
                args);
        Map<Long, TrackSummary> byId = playable.stream()
                .collect(Collectors.toMap(TrackSummary::id, Function.identity()));
        return trackIds.stream().map(byId::get).filter(Objects::nonNull).toList();
    }

    @Override
    @Transactional
    public TrackSummary recordPlay(long trackId) {
        int updated = jdbc.update(
                "UPDATE tracks SET play_count = play_count + 1, version = version + 1 "
                        + "WHERE id = ? AND status = ?",
                trackId,
                TrackStatus.PUBLISHED.name());
        if (updated == 0) {
            playable(trackId);
            throw new CatalogStateException("Track playback could not be recorded");
        }
        return find(trackId);
    }

    public List<TrackSummary> search(String keyword, TrackSort sort) {
        String normalized = keyword == null ? "" : keyword.trim().toLowerCase();
        String order = sort == TrackSort.POPULAR
                ? "t.play_count DESC, t.created_at DESC, t.id DESC"
                : "t.created_at DESC, t.id DESC";
        return jdbc.query(
                "SELECT " + TRACK_COLUMNS
                        + "FROM tracks t LEFT JOIN artist_profiles a ON a.user_id = t.artist_user_id "
                        + "WHERE t.status = ? AND (LOWER(t.title) LIKE ? "
                        + "OR LOWER(COALESCE(a.stage_name, '')) LIKE ?) ORDER BY " + order,
                trackMapper(),
                TrackStatus.PUBLISHED.name(), "%" + normalized + "%", "%" + normalized + "%");
    }

    @Override
    public List<ArtistSummary> searchArtists(String keyword) {
        String normalized = keyword == null ? "" : keyword.trim().toLowerCase();
        return jdbc.query(
                "SELECT user_id, stage_name, biography FROM artist_profiles "
                        + "WHERE status = ? AND LOWER(stage_name) LIKE ? ORDER BY stage_name ASC, user_id ASC",
                (rs, rowNum) -> new ArtistSummary(
                        rs.getLong("user_id"),
                        rs.getString("stage_name"),
                        rs.getString("biography")),
                "ACTIVE",
                "%" + normalized + "%");
    }

    @Override
    public List<TrackSummary> publishedByArtist(long userId) {
        return jdbc.query(
                "SELECT " + TRACK_COLUMNS
                        + "FROM tracks t LEFT JOIN artist_profiles a ON a.user_id = t.artist_user_id "
                        + "WHERE t.artist_user_id = ? AND t.status = ? "
                        + "ORDER BY t.created_at DESC, t.id DESC",
                trackMapper(),
                userId,
                TrackStatus.PUBLISHED.name());
    }

    @Override
    public TrackPage adminTracks(TrackStatus status, int page, int size) {
        Long total = jdbc.queryForObject(
                "SELECT COUNT(*) FROM tracks WHERE status = ?",
                Long.class,
                status.name());
        long totalItems = total == null ? 0L : total;
        List<TrackSummary> items = jdbc.query(
                "SELECT " + TRACK_COLUMNS
                        + "FROM tracks t LEFT JOIN artist_profiles a ON a.user_id = t.artist_user_id "
                        + "WHERE t.status = ? ORDER BY t.created_at DESC, t.id DESC LIMIT ? OFFSET ?",
                trackMapper(),
                status.name(),
                size,
                Math.multiplyExact(page, size));
        int totalPages = totalItems == 0L ? 0 : Math.toIntExact((totalItems + size - 1L) / size);
        return new TrackPage(items, page, size, totalItems, totalPages);
    }

    @Override
    @Transactional
    public ArtistSummary upsertArtist(long singerUserId, String stageName, String biography) {
        if (stageName == null || stageName.isBlank() || stageName.length() > 100
                || (biography != null && biography.length() > 2000)) {
            throw new CatalogStateException("Artist profile is invalid");
        }
        String normalizedStageName = stageName.trim();
        String normalizedBiography = biography == null || biography.isBlank()
                ? null
                : biography.trim();
        int updated = jdbc.update(
                "UPDATE artist_profiles SET stage_name = ?, biography = ?, status = ?, version = version + 1 "
                        + "WHERE user_id = ?",
                normalizedStageName,
                normalizedBiography,
                "ACTIVE",
                singerUserId);
        if (updated == 0) {
            jdbc.update(
                    "INSERT INTO artist_profiles (user_id, stage_name, biography, status) VALUES (?, ?, ?, ?)",
                    singerUserId,
                    normalizedStageName,
                    normalizedBiography,
                    "ACTIVE");
        }
        return findArtist(singerUserId);
    }

    @Override
    public ArtistSummary findArtist(long userId) {
        return jdbc.queryForObject(
                "SELECT user_id, stage_name, biography FROM artist_profiles WHERE user_id = ?",
                (rs, rowNum) -> new ArtistSummary(
                        rs.getLong("user_id"),
                        rs.getString("stage_name"),
                        rs.getString("biography")),
                userId);
    }

    private static void requireRole(Role actual, Role expected) {
        if (actual != expected) {
            throw new CatalogAuthorizationException();
        }
    }

    private static RowMapper<TrackSummary> trackMapper() {
        return (rs, rowNum) -> new TrackSummary(
                rs.getLong("id"),
                rs.getLong("artist_user_id"),
                rs.getString("title"),
                rs.getLong("media_object_id"),
                rs.getLong("duration_ms"),
                TrackStatus.valueOf(rs.getString("status")),
                rs.getLong("play_count"),
                rs.getTimestamp("created_at").toInstant(),
                rs.getObject("cover_media_object_id", Long.class),
                rs.getObject("lyrics_media_object_id", Long.class),
                rs.getString("artist_stage_name"));
    }
}
