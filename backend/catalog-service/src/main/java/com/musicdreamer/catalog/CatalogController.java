package com.musicdreamer.catalog;

import com.musicdreamer.security.AccountStatus;
import com.musicdreamer.security.AuthenticatedPrincipal;
import com.musicdreamer.security.Role;
import com.musicdreamer.security.TrustedIdentityHeaders;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/catalog")
public final class CatalogController {

    private final CatalogOperations catalog;

    public CatalogController(CatalogOperations catalog) {
        this.catalog = catalog;
    }

    @PostMapping("/tracks/drafts")
    public ResponseEntity<TrackSummary> createDraft(
            @RequestHeader HttpHeaders headers,
            @Valid @RequestBody DraftRequest request) {
        AuthenticatedPrincipal principal = require(headers, Role.SINGER);
        TrackSummary created = catalog.createDraft(
                principal.userId(),
                request.title(),
                request.mediaObjectId(),
                request.durationMs(),
                request.coverMediaObjectId(),
                request.lyricsMediaObjectId(),
                principal.role());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/tracks/{trackId}/submit")
    public TrackSummary submit(@RequestHeader HttpHeaders headers, @PathVariable long trackId) {
        AuthenticatedPrincipal principal = require(headers, Role.SINGER);
        return catalog.submit(principal.userId(), trackId);
    }

    @PutMapping("/tracks/{trackId}")
    public TrackSummary updateDraft(
            @RequestHeader HttpHeaders headers,
            @PathVariable long trackId,
            @Valid @RequestBody DraftRequest request) {
        AuthenticatedPrincipal principal = require(headers, Role.SINGER);
        return catalog.updateDraft(
                principal.userId(),
                trackId,
                request.title(),
                request.durationMs(),
                request.coverMediaObjectId(),
                request.lyricsMediaObjectId());
    }

    @PostMapping("/admin/tracks/{trackId}/publish")
    public TrackSummary publish(@RequestHeader HttpHeaders headers, @PathVariable long trackId) {
        AuthenticatedPrincipal principal = require(headers, Role.ADMIN);
        return catalog.publish(principal.userId(), trackId);
    }

    @PostMapping("/admin/tracks/{trackId}/lock")
    public TrackSummary lock(@RequestHeader HttpHeaders headers, @PathVariable long trackId) {
        AuthenticatedPrincipal principal = require(headers, Role.ADMIN);
        return catalog.lock(principal.userId(), trackId);
    }

    @GetMapping("/admin/tracks")
    public TrackPage adminTracks(
            @RequestHeader HttpHeaders headers,
            @RequestParam String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        require(headers, Role.ADMIN);
        if (page < 0 || size < 1 || size > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Page must be non-negative and size must be 1-100");
        }
        return catalog.adminTracks(parseAdminStatus(status), page, size);
    }

    @PutMapping("/artists/me")
    public ArtistSummary updateArtist(
            @RequestHeader HttpHeaders headers,
            @Valid @RequestBody ArtistRequest request) {
        AuthenticatedPrincipal principal = require(headers, Role.SINGER);
        return catalog.upsertArtist(principal.userId(), request.stageName(), request.biography());
    }

    @GetMapping("/artists/{userId}")
    public ArtistSummary findArtist(
            @RequestHeader HttpHeaders headers,
            @PathVariable long userId) {
        requireActive(headers);
        return catalog.findArtist(userId);
    }

    @GetMapping("/tracks/search")
    public List<TrackSummary> search(
            @RequestHeader HttpHeaders headers,
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "LATEST") TrackSort sort) {
        requireActive(headers);
        return catalog.search(keyword, sort);
    }

    @GetMapping("/tracks/{trackId}")
    public TrackSummary find(@RequestHeader HttpHeaders headers, @PathVariable long trackId) {
        requireActive(headers);
        return catalog.find(trackId);
    }

    @GetMapping("/tracks/{trackId}/playable")
    public TrackSummary playable(@RequestHeader HttpHeaders headers, @PathVariable long trackId) {
        requireActive(headers);
        return catalog.playable(trackId);
    }

    private static AuthenticatedPrincipal require(HttpHeaders headers, Role role) {
        AuthenticatedPrincipal principal = requireActive(headers);
        if (principal.role() != role) {
            throw new CatalogAuthorizationException();
        }
        return principal;
    }

    private static AuthenticatedPrincipal requireActive(HttpHeaders headers) {
        AuthenticatedPrincipal principal = TrustedIdentityHeaders.principalFrom(headers)
                .orElseThrow(CatalogAuthorizationException::new);
        if (principal.status() != AccountStatus.ACTIVE) {
            throw new CatalogAuthorizationException();
        }
        return principal;
    }

    private static TrackStatus parseAdminStatus(String value) {
        if ("SUBMITTED".equals(value)) {
            return TrackStatus.PENDING_REVIEW;
        }
        try {
            return TrackStatus.valueOf(value);
        } catch (IllegalArgumentException failure) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown track status", failure);
        }
    }

    public record DraftRequest(
            @NotBlank @Size(max = 200) String title,
            @Positive long mediaObjectId,
            @Positive long durationMs,
            @Positive Long coverMediaObjectId,
            @Positive Long lyricsMediaObjectId) {

        public DraftRequest(String title, long mediaObjectId, long durationMs) {
            this(title, mediaObjectId, durationMs, null, null);
        }
    }

    public record ArtistRequest(
            @NotBlank @Size(max = 100) String stageName,
            @Size(max = 2000) String biography) {
    }
}
