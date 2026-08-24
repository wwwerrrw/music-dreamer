package com.musicdreamer.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import com.musicdreamer.security.Role;
import org.flywaydb.core.Flyway;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.KeyHolder;

class CatalogApplicationServiceTest {

    private static final AtomicInteger DATABASES = new AtomicInteger();

    private CatalogApplicationService service;

    @BeforeEach
    void setUp() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:catalog-service-" + DATABASES.incrementAndGet()
                + ";MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE");
        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration/catalog")
                .load()
                .migrate();
        service = new CatalogApplicationService(new JdbcTemplate(dataSource));
    }

    @Test
    void singerCreatesAndSubmitsOwnDraftThenAdminPublishesIt() {
        TrackSummary draft = service.createDraft(9L, "Blue Hour", 101L, 180_000L);
        assertThat(draft.status()).isEqualTo(TrackStatus.DRAFT);

        TrackSummary pending = service.submit(9L, draft.id());
        assertThat(pending.status()).isEqualTo(TrackStatus.PENDING_REVIEW);

        TrackSummary published = service.publish(1L, draft.id());
        assertThat(published.status()).isEqualTo(TrackStatus.PUBLISHED);
        assertThat(service.search("blue", TrackSort.LATEST)).extracting(TrackSummary::id)
                .containsExactly(draft.id());
    }

    @Test
    void nonSingerCannotCreateAndOtherSingerCannotSubmit() {
        assertThatThrownBy(() -> service.createDraft(9L, "Blue Hour", 101L, 180_000L, Role.USER))
                .isInstanceOf(CatalogAuthorizationException.class);
        TrackSummary draft = service.createDraft(9L, "Blue Hour", 101L, 180_000L);
        assertThatThrownBy(() -> service.submit(10L, draft.id()))
                .isInstanceOf(CatalogAuthorizationException.class);
    }

    @Test
    void singerCanEditOwnDraftButNotAnotherSingersDraft() {
        TrackSummary draft = service.createDraft(9L, "Old", 101L, 180_000L);

        TrackSummary updated = service.updateDraft(9L, draft.id(), "New", 190_000L);

        assertThat(updated.title()).isEqualTo("New");
        assertThat(updated.durationMs()).isEqualTo(190_000L);
        assertThatThrownBy(() -> service.updateDraft(10L, draft.id(), "Nope", 1L))
                .isInstanceOf(CatalogAuthorizationException.class);
    }

    @Test
    void publishedSearchUsesDeterministicLatestOrder() {
        TrackSummary first = service.createDraft(9L, "Same", 101L, 1L);
        service.submit(9L, first.id());
        service.publish(1L, first.id());
        TrackSummary second = service.createDraft(9L, "Same", 102L, 1L);
        service.submit(9L, second.id());
        service.publish(1L, second.id());

        List<TrackSummary> results = service.search("same", TrackSort.LATEST);
        assertThat(results).extracting(TrackSummary::id).containsExactly(second.id(), first.id());
    }

    @Test
    void searchFindsPublishedTracksByArtistNameAndExposesArtistMatches() {
        service.upsertArtist(9L, "North Shore", "Writes after midnight.");
        TrackSummary draft = service.createDraft(9L, "Blue Hour", 101L, 1L);
        service.submit(9L, draft.id());
        service.publish(1L, draft.id());

        assertThat(service.search("north", TrackSort.LATEST))
                .extracting(TrackSummary::id)
                .containsExactly(draft.id());
        assertThat(service.searchArtists("shore"))
                .containsExactly(new ArtistSummary(9L, "North Shore", "Writes after midnight."));
    }

    @Test
    void publishedArtistTracksExcludeDraftsAndOtherSingers() {
        TrackSummary published = service.createDraft(9L, "Published", 101L, 1L);
        service.submit(9L, published.id());
        service.publish(1L, published.id());
        service.createDraft(9L, "Draft", 102L, 1L);
        TrackSummary other = service.createDraft(10L, "Other", 103L, 1L);
        service.submit(10L, other.id());
        service.publish(1L, other.id());

        assertThat(service.publishedByArtist(9L))
                .extracting(TrackSummary::id)
                .containsExactly(published.id());
    }

    @Test
    void adminTrackPageFiltersByStatusAndUsesStableLatestOrder() {
        TrackSummary first = service.createDraft(9L, "First pending", 101L, 1L);
        service.submit(9L, first.id());
        TrackSummary second = service.createDraft(9L, "Second pending", 102L, 1L);
        service.submit(9L, second.id());
        TrackSummary published = service.createDraft(9L, "Published", 103L, 1L);
        service.submit(9L, published.id());
        service.publish(1L, published.id());

        TrackPage firstPage = service.adminTracks(TrackStatus.PENDING_REVIEW, 0, 1);
        TrackPage secondPage = service.adminTracks(TrackStatus.PENDING_REVIEW, 1, 1);

        assertThat(firstPage.items()).extracting(TrackSummary::id).containsExactly(second.id());
        assertThat(secondPage.items()).extracting(TrackSummary::id).containsExactly(first.id());
        assertThat(firstPage.totalItems()).isEqualTo(2);
        assertThat(firstPage.totalPages()).isEqualTo(2);
    }

    @Test
    void artistProfileIsPersistedAndUpdatedForTheSameSinger() {
        service.upsertArtist(9L, " North Shore ", "Writes after midnight.");

        assertThat(service.findArtist(9L)).isEqualTo(
                new ArtistSummary(9L, "North Shore", "Writes after midnight."));

        service.upsertArtist(9L, "Northern Lights", null);

        assertThat(service.findArtist(9L)).isEqualTo(
                new ArtistSummary(9L, "Northern Lights", null));
    }

    @Test
    void draftPersistsOptionalMediaAndReturnsTheArtistStageName() {
        service.upsertArtist(9L, "North Shore", null);

        TrackSummary draft = service.createDraft(
                9L,
                "Midnight Road",
                101L,
                180_000L,
                201L,
                202L,
                Role.SINGER);

        assertThat(draft.coverMediaObjectId()).isEqualTo(201L);
        assertThat(draft.lyricsMediaObjectId()).isEqualTo(202L);
        assertThat(draft.artistStageName()).isEqualTo("North Shore");
    }

    @Test
    void updatingDraftReplacesAndClearsOptionalMedia() {
        service.upsertArtist(9L, "North Shore", null);
        TrackSummary draft = service.createDraft(
                9L,
                "Midnight Road",
                101L,
                180_000L,
                201L,
                202L,
                Role.SINGER);

        service.updateDraft(9L, draft.id(), "Afterglow", 190_000L, 211L, null);
        TrackSummary updated = service.find(draft.id());

        assertThat(updated.title()).isEqualTo("Afterglow");
        assertThat(updated.durationMs()).isEqualTo(190_000L);
        assertThat(updated.coverMediaObjectId()).isEqualTo(211L);
        assertThat(updated.lyricsMediaObjectId()).isNull();
        assertThat(updated.artistStageName()).isEqualTo("North Shore");
    }

    @Test
    void lockedTrackCannotBeRequestedForPlayback() {
        TrackSummary draft = service.createDraft(9L, "Locked", 101L, 1L);
        service.submit(9L, draft.id());
        service.publish(1L, draft.id());
        service.lock(1L, draft.id());

        assertThatThrownBy(() -> service.playable(draft.id()))
                .isInstanceOf(CatalogStateException.class);
    }

    @Test
    void singerRemovesOwnPublishedTrackFromPublicCatalog() {
        TrackSummary draft = service.createDraft(9L, "Last Light", 101L, 1L);
        service.submit(9L, draft.id());
        service.publish(1L, draft.id());

        service.removePublished(9L, draft.id());

        assertThat(service.find(draft.id()).status().name()).isEqualTo("REMOVED");
        assertThat(service.search("last", TrackSort.LATEST)).isEmpty();
        assertThat(service.publishedByArtist(9L)).isEmpty();
        assertThatThrownBy(() -> service.playable(draft.id()))
                .isInstanceOf(CatalogStateException.class);
    }

    @Test
    void singerCannotRemoveAnotherSingersTrackOrAnUnpublishedTrack() {
        TrackSummary published = service.createDraft(9L, "Owned", 101L, 1L);
        service.submit(9L, published.id());
        service.publish(1L, published.id());
        TrackSummary draft = service.createDraft(9L, "Still drafting", 102L, 1L);

        assertThatThrownBy(() -> service.removePublished(10L, published.id()))
                .isInstanceOf(CatalogAuthorizationException.class);
        assertThatThrownBy(() -> service.removePublished(9L, draft.id()))
                .isInstanceOf(CatalogStateException.class);
        assertThat(service.find(published.id()).status()).isEqualTo(TrackStatus.PUBLISHED);
        assertThat(service.find(draft.id()).status()).isEqualTo(TrackStatus.DRAFT);
    }

    @Test
    void recordingPlaybackAtomicallyIncrementsOnlyPublishedTrackCount() {
        TrackSummary draft = service.createDraft(9L, "Counted", 101L, 1L);
        service.submit(9L, draft.id());
        service.publish(1L, draft.id());

        assertThat(service.recordPlay(draft.id()).playCount()).isEqualTo(1L);
        assertThat(service.recordPlay(draft.id()).playCount()).isEqualTo(2L);

        TrackSummary locked = service.createDraft(9L, "Locked count", 102L, 1L);
        service.submit(9L, locked.id());
        service.publish(1L, locked.id());
        service.lock(1L, locked.id());
        assertThatThrownBy(() -> service.recordPlay(locked.id()))
                .isInstanceOf(CatalogStateException.class);
    }

    @Test
    void createsDraftWhenMysqlLabelsTheGeneratedIdAsGeneratedKey() {
        JdbcTemplate mysqlLikeJdbc = new JdbcTemplate() {
            @Override
            public <T> T queryForObject(String sql, Class<T> requiredType, Object... args) {
                return requiredType.cast(1);
            }

            @Override
            public int update(PreparedStatementCreator creator, KeyHolder keys) {
                keys.getKeyList().add(Map.of("GENERATED_KEY", 88L));
                return 1;
            }
        };
        CatalogApplicationService mysqlLikeService = new CatalogApplicationService(mysqlLikeJdbc) {
            @Override
            public TrackSummary find(long trackId) {
                return new TrackSummary(trackId, 9L, "MySQL", 101L, 1L,
                        TrackStatus.DRAFT, 0L, java.time.Instant.EPOCH);
            }
        };

        TrackSummary draft = mysqlLikeService.createDraft(9L, "MySQL", 101L, 1L);

        assertThat(draft.id()).isEqualTo(88L);
    }
}
