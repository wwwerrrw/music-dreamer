package com.musicdreamer.interaction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.musicdreamer.security.Role;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.flywaydb.core.Flyway;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class InteractionApplicationServiceTest {

    private static final AtomicInteger DATABASES = new AtomicInteger();

    private InteractionApplicationService service;
    private JdbcTemplate jdbc;

    @BeforeEach
    void setUp() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:interaction-service-" + DATABASES.incrementAndGet()
                + ";MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE");
        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration/interaction")
                .load()
                .migrate();
        jdbc = new JdbcTemplate(dataSource);
        service = new InteractionApplicationService(jdbc);
    }

    @Test
    void duplicateFavoriteDoesNotCreateDuplicateRelationship() {
        service.addFavorite(14L, 2L);
        service.addFavorite(14L, 2L);

        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM track_favorites WHERE user_id = ? AND track_id = ?",
                Integer.class, 14L, 2L)).isEqualTo(1);
    }

    @Test
    void playlistOwnerCanAddTrackButOtherUserIsForbidden() {
        PlaylistSummary playlist = service.createPlaylist(14L, "Road Trip", "Songs", "PRIVATE");

        service.addTrack(14L, playlist.id(), 2L, 1);
        service.addTrack(14L, playlist.id(), 2L, 1);

        assertThatThrownBy(() -> service.addTrack(15L, playlist.id(), 3L, 2))
                .isInstanceOf(InteractionAuthorizationException.class);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM playlist_tracks WHERE playlist_id = ?",
                Integer.class, playlist.id())).isEqualTo(1);
    }

    @Test
    void listeningHistoryUpdatesRecentPositionInsteadOfDuplicatingRows() {
        service.updateHistory(14L, 2L, 100L);
        service.updateHistory(14L, 2L, 250L);

        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM listening_history WHERE user_id = ? AND track_id = ?",
                Integer.class, 14L, 2L)).isEqualTo(1);
        assertThat(jdbc.queryForObject(
                "SELECT position_ms FROM listening_history WHERE user_id = ? AND track_id = ?",
                Long.class, 14L, 2L)).isEqualTo(250L);
    }

    @Test
    void adminBroadcastAppearsInInboxAndReadIsIdempotent() {
        MessageSummary message = service.publishBroadcast(13L, Role.ADMIN, "Maintenance", "Back soon");

        List<MessageSummary> inbox = service.inbox(14L);
        assertThat(inbox).extracting(MessageSummary::id).containsExactly(message.id());
        assertThat(inbox.getFirst().readAt()).isNull();

        service.markRead(14L, message.id());
        service.markRead(14L, message.id());

        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM message_receipts WHERE message_id = ? AND user_id = ?",
                Integer.class, message.id(), 14L)).isEqualTo(1);
        assertThatThrownBy(() -> service.publishBroadcast(14L, Role.USER, "Nope", "Nope"))
                .isInstanceOf(InteractionAuthorizationException.class);
    }
}
