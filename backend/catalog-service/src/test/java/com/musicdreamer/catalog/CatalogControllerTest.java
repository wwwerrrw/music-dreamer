package com.musicdreamer.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.nullValue;

import com.musicdreamer.security.AccountStatus;
import com.musicdreamer.security.AuthenticatedPrincipal;
import com.musicdreamer.security.Role;
import com.musicdreamer.security.TrustedIdentityHeaders;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class CatalogControllerTest {

    @Test
    void singerCanMaintainTheirOwnArtistProfile() throws Exception {
        MockMvc mvc = mvc(new CatalogOperationsStub());

        mvc.perform(put("/api/catalog/artists/me")
                        .headers(headers(new AuthenticatedPrincipal(
                                9L, Role.SINGER, AccountStatus.ACTIVE)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"stageName":"North Shore","biography":"Writes after midnight."}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(9))
                .andExpect(jsonPath("$.stageName").value("North Shore"))
                .andExpect(jsonPath("$.biography").value("Writes after midnight."));
    }

    @Test
    void activeUserCanReadAnArtistProfile() throws Exception {
        MockMvc mvc = mvc(new CatalogOperationsStub());

        mvc.perform(get("/api/catalog/artists/9")
                        .headers(headers(new AuthenticatedPrincipal(
                                7L, Role.USER, AccountStatus.ACTIVE))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(9))
                .andExpect(jsonPath("$.stageName").value("North Shore"));
    }

    @Test
    void ordinaryUserCannotMaintainAnArtistProfile() throws Exception {
        MockMvc mvc = mvc(new CatalogOperationsStub());

        mvc.perform(put("/api/catalog/artists/me")
                        .headers(headers(new AuthenticatedPrincipal(
                                7L, Role.USER, AccountStatus.ACTIVE)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"stageName":"Not a singer","biography":null}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void trackResponseIncludesOptionalMediaAndArtistFields() throws Exception {
        MockMvc mvc = mvc(new CatalogOperationsStub());

        mvc.perform(get("/api/catalog/tracks/search")
                        .headers(headers(new AuthenticatedPrincipal(
                                7L, Role.USER, AccountStatus.ACTIVE)))
                        .queryParam("keyword", "song"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].coverMediaObjectId").value(nullValue()))
                .andExpect(jsonPath("$[0].lyricsMediaObjectId").value(nullValue()))
                .andExpect(jsonPath("$[0].artistStageName").value(nullValue()));
    }

    @Test
    void activeUserCanRecordAPlaybackStart() throws Exception {
        MockMvc mvc = mvc(new CatalogOperationsStub());

        mvc.perform(post("/api/catalog/tracks/1/plays")
                        .headers(headers(new AuthenticatedPrincipal(
                                7L, Role.USER, AccountStatus.ACTIVE))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.playCount").value(1));
    }

    @Test
    void publicTrackDetailOnlyReturnsAPlayableTrack() throws Exception {
        MockMvc mvc = mvc(new CatalogOperationsStub());

        mvc.perform(get("/api/catalog/tracks/1")
                        .headers(headers(new AuthenticatedPrincipal(
                                7L, Role.USER, AccountStatus.ACTIVE))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));
    }

    @Test
    void singerDraftRequestCarriesOptionalCoverAndLyricsMedia() throws Exception {
        MockMvc mvc = mvc(new CatalogOperationsStub());

        mvc.perform(post("/api/catalog/tracks/drafts")
                        .headers(headers(new AuthenticatedPrincipal(
                                9L, Role.SINGER, AccountStatus.ACTIVE)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title":"Song",
                                  "mediaObjectId":3,
                                  "durationMs":120000,
                                  "coverMediaObjectId":201,
                                  "lyricsMediaObjectId":202
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.coverMediaObjectId").value(201))
                .andExpect(jsonPath("$.lyricsMediaObjectId").value(202));
    }

    @Test
    void singerDraftUpdateCanReplaceAndClearOptionalMedia() throws Exception {
        MockMvc mvc = mvc(new CatalogOperationsStub());

        mvc.perform(put("/api/catalog/tracks/1")
                        .headers(headers(new AuthenticatedPrincipal(
                                9L, Role.SINGER, AccountStatus.ACTIVE)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title":"Updated song",
                                  "mediaObjectId":3,
                                  "durationMs":125000,
                                  "coverMediaObjectId":211,
                                  "lyricsMediaObjectId":null
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.coverMediaObjectId").value(211))
                .andExpect(jsonPath("$.lyricsMediaObjectId").value(nullValue()));
    }

    @Test
    void singerCanRemoveTheirPublishedTrack() throws Exception {
        MockMvc mvc = mvc(new CatalogOperationsStub());

        mvc.perform(delete("/api/catalog/tracks/1")
                        .headers(headers(new AuthenticatedPrincipal(
                                9L, Role.SINGER, AccountStatus.ACTIVE))))
                .andExpect(status().isNoContent());
    }

    @Test
    void adminCanListSubmittedTracksWithStablePagination() throws Exception {
        MockMvc mvc = mvc(new CatalogOperationsStub());

        mvc.perform(get("/api/catalog/admin/tracks")
                        .headers(headers(new AuthenticatedPrincipal(
                                1L, Role.ADMIN, AccountStatus.ACTIVE)))
                        .queryParam("status", "SUBMITTED")
                        .queryParam("page", "0")
                        .queryParam("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(1))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void ordinaryUserCannotListAdminTracks() throws Exception {
        MockMvc mvc = mvc(new CatalogOperationsStub());

        mvc.perform(get("/api/catalog/admin/tracks")
                        .headers(headers(new AuthenticatedPrincipal(
                                7L, Role.USER, AccountStatus.ACTIVE)))
                        .queryParam("status", "SUBMITTED"))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminTrackListRejectsPaginationOutsideTheContract() throws Exception {
        MockMvc mvc = mvc(new CatalogOperationsStub());

        mvc.perform(get("/api/catalog/admin/tracks")
                        .headers(headers(new AuthenticatedPrincipal(
                                1L, Role.ADMIN, AccountStatus.ACTIVE)))
                        .queryParam("status", "SUBMITTED")
                        .queryParam("page", "-1")
                        .queryParam("size", "101"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void controllerRejectsOrdinaryUserFromSingerDraftEndpoint() {
        CatalogController controller = new CatalogController(null);
        HttpHeaders headers = headers(new AuthenticatedPrincipal(7L, Role.USER, AccountStatus.ACTIVE));

        assertThatThrownBy(() -> controller.createDraft(headers,
                new CatalogController.DraftRequest("Song", 2L, 120_000L)))
                .isInstanceOf(CatalogAuthorizationException.class);
    }

    @Test
    void controllerReturnsSearchResultsForAuthenticatedUser() {
        CatalogController controller = new CatalogController(new CatalogOperationsStub());
        HttpHeaders headers = headers(new AuthenticatedPrincipal(7L, Role.USER, AccountStatus.ACTIVE));

        assertThat(controller.search(headers, "song", TrackSort.LATEST)).containsExactly(
                new TrackSummary(1L, 2L, "Song", 3L, 120_000L, TrackStatus.PUBLISHED, 0L, null));
    }

    private static HttpHeaders headers(AuthenticatedPrincipal principal) {
        return TrustedIdentityHeaders.forPrincipal(new HttpHeaders(), principal);
    }

    private static MockMvc mvc(CatalogOperations operations) {
        return MockMvcBuilders.standaloneSetup(new CatalogController(operations))
                .setControllerAdvice(new CatalogExceptionHandler())
                .build();
    }

    private static final class CatalogOperationsStub implements CatalogOperations {

        @Override
        public List<TrackSummary> search(String keyword, TrackSort sort) {
            return List.of(new TrackSummary(1L, 2L, "Song", 3L, 120_000L,
                    TrackStatus.PUBLISHED, 0L, null));
        }

        @Override
        public List<ArtistSummary> searchArtists(String keyword) {
            return List.of(new ArtistSummary(2L, "North Shore", null));
        }

        @Override
        public List<TrackSummary> publishedByArtist(long userId) {
            return search("", TrackSort.LATEST);
        }

        @Override
        public TrackPage adminTracks(TrackStatus status, int page, int size) {
            assertThat(status).isEqualTo(TrackStatus.PENDING_REVIEW);
            return new TrackPage(
                    List.of(new TrackSummary(1L, 2L, "Song", 3L, 120_000L,
                            TrackStatus.PENDING_REVIEW, 0L, null)),
                    page,
                    size,
                    1L,
                    1);
        }

        @Override
        public ArtistSummary upsertArtist(long singerUserId, String stageName, String biography) {
            return new ArtistSummary(singerUserId, stageName, biography);
        }

        @Override
        public ArtistSummary findArtist(long userId) {
            return new ArtistSummary(userId, "North Shore", "Writes after midnight.");
        }

        @Override
        public TrackSummary createDraft(long singerUserId, String title, long mediaObjectId, long durationMs, Role role) {
            return new TrackSummary(1L, singerUserId, title, mediaObjectId, durationMs,
                    TrackStatus.DRAFT, 0L, null);
        }

        @Override
        public TrackSummary createDraft(
                long singerUserId,
                String title,
                long mediaObjectId,
                long durationMs,
                Long coverMediaObjectId,
                Long lyricsMediaObjectId,
                Role role) {
            return new TrackSummary(
                    1L,
                    singerUserId,
                    title,
                    mediaObjectId,
                    durationMs,
                    TrackStatus.DRAFT,
                    0L,
                    null,
                    coverMediaObjectId,
                    lyricsMediaObjectId,
                    null);
        }

        @Override
        public TrackSummary submit(long singerUserId, long trackId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public TrackSummary updateDraft(long singerUserId, long trackId, String title, long durationMs) {
            return new TrackSummary(trackId, singerUserId, title, 3L, durationMs,
                    TrackStatus.DRAFT, 0L, null);
        }

        @Override
        public TrackSummary updateDraft(
                long singerUserId,
                long trackId,
                String title,
                long durationMs,
                Long coverMediaObjectId,
                Long lyricsMediaObjectId) {
            return new TrackSummary(
                    trackId,
                    singerUserId,
                    title,
                    3L,
                    durationMs,
                    TrackStatus.DRAFT,
                    0L,
                    null,
                    coverMediaObjectId,
                    lyricsMediaObjectId,
                    null);
        }

        @Override
        public void removePublished(long singerUserId, long trackId) {
        }

        @Override
        public TrackSummary publish(long adminUserId, long trackId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public TrackSummary lock(long adminUserId, long trackId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public TrackSummary find(long trackId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public TrackSummary playable(long trackId) {
            return new TrackSummary(trackId, 2L, "Song", 3L, 120_000L,
                    TrackStatus.PUBLISHED, 0L, null);
        }

        @Override
        public TrackSummary recordPlay(long trackId) {
            return new TrackSummary(trackId, 2L, "Song", 3L, 120_000L,
                    TrackStatus.PUBLISHED, 1L, null);
        }
    }
}
