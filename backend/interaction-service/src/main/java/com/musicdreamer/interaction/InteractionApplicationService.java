package com.musicdreamer.interaction;

import com.musicdreamer.security.Role;
import java.util.List;
import java.util.Objects;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.transaction.annotation.Transactional;

public class InteractionApplicationService {

    private final JdbcTemplate jdbc;

    public InteractionApplicationService(JdbcTemplate jdbc) {
        this.jdbc = Objects.requireNonNull(jdbc, "jdbc");
    }

    @Transactional
    public void addFavorite(long userId, long trackId) {
        try {
            jdbc.update(
                    "INSERT INTO track_favorites (user_id, track_id, version) VALUES (?, ?, 0)",
                    userId, trackId);
        } catch (DuplicateKeyException ignored) {
            // The unique relationship makes repeated favorite requests idempotent.
        }
    }

    @Transactional
    public void removeFavorite(long userId, long trackId) {
        jdbc.update("DELETE FROM track_favorites WHERE user_id = ? AND track_id = ?", userId, trackId);
    }

    public List<FavoriteSummary> favorites(long userId) {
        return jdbc.query(
                "SELECT id, user_id, track_id, created_at FROM track_favorites "
                        + "WHERE user_id = ? ORDER BY created_at DESC, id DESC",
                (rs, rowNum) -> new FavoriteSummary(
                        rs.getLong("id"),
                        rs.getLong("user_id"),
                        rs.getLong("track_id"),
                        rs.getTimestamp("created_at").toInstant()),
                userId);
    }

    @Transactional
    public PlaylistSummary createPlaylist(long ownerUserId, String name, String description, String visibility) {
        validatePlaylist(name, visibility);
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            var statement = connection.prepareStatement(
                    "INSERT INTO playlists (owner_user_id, name, description, visibility, version) VALUES (?, ?, ?, ?, 0)",
                    new String[] {"id"});
            statement.setLong(1, ownerUserId);
            statement.setString(2, name);
            statement.setString(3, description);
            statement.setString(4, visibility);
            return statement;
        }, keyHolder);
        Number generatedId = keyHolder.getKey();
        if (generatedId == null) {
            throw new IllegalStateException("Playlist insert did not return an id");
        }
        return findPlaylist(generatedId.longValue());
    }

    public List<PlaylistSummary> playlists(long ownerUserId) {
        return jdbc.query(
                "SELECT id, owner_user_id, name, description, visibility FROM playlists "
                        + "WHERE owner_user_id = ? ORDER BY updated_at DESC, id DESC",
                (rs, rowNum) -> toPlaylistSummary(rs),
                ownerUserId);
    }

    public PlaylistDetails playlist(long userId, long playlistId) {
        PlaylistSummary playlist = findPlaylist(playlistId);
        requireOwner(userId, playlist);
        List<PlaylistTrackSummary> tracks = jdbc.query(
                "SELECT track_id, sort_order, created_at FROM playlist_tracks "
                        + "WHERE playlist_id = ? ORDER BY sort_order ASC, id ASC",
                (rs, rowNum) -> new PlaylistTrackSummary(
                        rs.getLong("track_id"),
                        rs.getInt("sort_order"),
                        rs.getTimestamp("created_at").toInstant()),
                playlistId);
        return new PlaylistDetails(playlist, tracks);
    }

    @Transactional
    public void addTrack(long userId, long playlistId, long trackId, int sortOrder) {
        requireOwner(userId, findPlaylist(playlistId));
        if (sortOrder < 0) {
            throw new IllegalArgumentException("Playlist sort order cannot be negative");
        }
        try {
            jdbc.update(
                    "INSERT INTO playlist_tracks (playlist_id, track_id, sort_order, version) VALUES (?, ?, ?, 0)",
                    playlistId, trackId, sortOrder);
        } catch (DuplicateKeyException ignored) {
            // The unique relationship makes repeated playlist additions idempotent.
        }
    }

    @Transactional
    public void removeTrack(long userId, long playlistId, long trackId) {
        requireOwner(userId, findPlaylist(playlistId));
        jdbc.update("DELETE FROM playlist_tracks WHERE playlist_id = ? AND track_id = ?", playlistId, trackId);
    }

    public List<ListeningHistorySummary> history(long userId) {
        return jdbc.query(
                "SELECT track_id, position_ms, last_listened_at FROM listening_history "
                        + "WHERE user_id = ? ORDER BY last_listened_at DESC, id DESC",
                (rs, rowNum) -> new ListeningHistorySummary(
                        rs.getLong("track_id"),
                        rs.getLong("position_ms"),
                        rs.getTimestamp("last_listened_at").toInstant()),
                userId);
    }

    @Transactional
    public void updateHistory(long userId, long trackId, long positionMs) {
        if (positionMs < 0) {
            throw new IllegalArgumentException("Playback position cannot be negative");
        }
        int updated = jdbc.update(
                "UPDATE listening_history SET position_ms = ?, last_listened_at = CURRENT_TIMESTAMP(6), "
                        + "version = version + 1, updated_at = CURRENT_TIMESTAMP(6) "
                        + "WHERE user_id = ? AND track_id = ?",
                positionMs, userId, trackId);
        if (updated > 0) {
            return;
        }
        try {
            jdbc.update(
                    "INSERT INTO listening_history "
                            + "(user_id, track_id, position_ms, last_listened_at, version) "
                            + "VALUES (?, ?, ?, CURRENT_TIMESTAMP(6), 0)",
                    userId, trackId, positionMs);
        } catch (DuplicateKeyException ignored) {
            jdbc.update(
                    "UPDATE listening_history SET position_ms = ?, last_listened_at = CURRENT_TIMESTAMP(6), "
                            + "version = version + 1, updated_at = CURRENT_TIMESTAMP(6) "
                            + "WHERE user_id = ? AND track_id = ?",
                    positionMs, userId, trackId);
        }
    }

    @Transactional
    public MessageSummary publishBroadcast(long senderUserId, Role role, String subject, String body) {
        if (role != Role.ADMIN) {
            throw new InteractionAuthorizationException();
        }
        if (subject == null || subject.isBlank() || subject.length() > 200
                || body == null || body.isBlank()) {
            throw new IllegalArgumentException("Message subject and body are required");
        }
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            var statement = connection.prepareStatement(
                    "INSERT INTO messages "
                            + "(sender_user_id, audience_type, subject, body, published_at, version) "
                            + "VALUES (?, 'ALL', ?, ?, CURRENT_TIMESTAMP(6), 0)",
                    new String[] {"id"});
            statement.setLong(1, senderUserId);
            statement.setString(2, subject);
            statement.setString(3, body);
            return statement;
        }, keyHolder);
        Number generatedId = keyHolder.getKey();
        if (generatedId == null) {
            throw new IllegalStateException("Message insert did not return an id");
        }
        return jdbc.queryForObject(
                "SELECT id, sender_user_id, audience_type, subject, body, published_at "
                        + "FROM messages WHERE id = ?",
                (rs, rowNum) -> new MessageSummary(
                        rs.getLong("id"),
                        (Long) rs.getObject("sender_user_id"),
                        rs.getString("audience_type"),
                        rs.getString("subject"),
                        rs.getString("body"),
                        rs.getTimestamp("published_at").toInstant(),
                        null),
                generatedId.longValue());
    }

    public List<MessageSummary> inbox(long userId) {
        return jdbc.query(
                "SELECT m.id, m.sender_user_id, m.audience_type, m.subject, m.body, m.published_at, r.read_at "
                        + "FROM messages m LEFT JOIN message_receipts r "
                        + "ON r.message_id = m.id AND r.user_id = ? "
                        + "WHERE m.audience_type = 'ALL' AND m.published_at IS NOT NULL "
                        + "ORDER BY m.published_at DESC, m.id DESC",
                (rs, rowNum) -> new MessageSummary(
                        rs.getLong("id"),
                        (Long) rs.getObject("sender_user_id"),
                        rs.getString("audience_type"),
                        rs.getString("subject"),
                        rs.getString("body"),
                        rs.getTimestamp("published_at").toInstant(),
                        rs.getTimestamp("read_at") == null ? null : rs.getTimestamp("read_at").toInstant()),
                userId);
    }

    @Transactional
    public void markRead(long userId, long messageId) {
        Integer published = jdbc.queryForObject(
                "SELECT COUNT(*) FROM messages WHERE id = ? AND audience_type = 'ALL' AND published_at IS NOT NULL",
                Integer.class,
                messageId);
        if (published == null || published == 0) {
            throw new InteractionNotFoundException("Message was not found");
        }
        try {
            jdbc.update(
                    "INSERT INTO message_receipts (message_id, user_id, read_at, version) "
                            + "VALUES (?, ?, CURRENT_TIMESTAMP(6), 0)",
                    messageId, userId);
        } catch (DuplicateKeyException ignored) {
            // A read receipt is a unique relationship and is therefore idempotent.
        }
    }

    private PlaylistSummary findPlaylist(long playlistId) {
        try {
            return jdbc.queryForObject(
                    "SELECT id, owner_user_id, name, description, visibility FROM playlists WHERE id = ?",
                    (rs, rowNum) -> toPlaylistSummary(rs),
                    playlistId);
        } catch (EmptyResultDataAccessException failure) {
            throw new InteractionNotFoundException("Playlist was not found");
        }
    }

    private static PlaylistSummary toPlaylistSummary(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new PlaylistSummary(
                rs.getLong("id"),
                rs.getLong("owner_user_id"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getString("visibility"));
    }

    private static void requireOwner(long userId, PlaylistSummary playlist) {
        if (playlist.ownerUserId() != userId) {
            throw new InteractionAuthorizationException();
        }
    }

    private static void validatePlaylist(String name, String visibility) {
        if (name == null || name.isBlank() || name.length() > 120
                || visibility == null || visibility.isBlank() || visibility.length() > 16) {
            throw new IllegalArgumentException("Playlist name and visibility are invalid");
        }
    }
}
