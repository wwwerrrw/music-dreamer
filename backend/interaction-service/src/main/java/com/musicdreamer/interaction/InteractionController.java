package com.musicdreamer.interaction;

import com.musicdreamer.security.AccountStatus;
import com.musicdreamer.security.AuthenticatedPrincipal;
import com.musicdreamer.security.Role;
import com.musicdreamer.security.TrustedIdentityHeaders;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/interaction")
public final class InteractionController {

    private final InteractionApplicationService interactions;

    public InteractionController(InteractionApplicationService interactions) {
        this.interactions = interactions;
    }

    @PutMapping("/favorites/{trackId}")
    public ResponseEntity<Void> addFavorite(
            @RequestHeader HttpHeaders headers,
            @PathVariable @Positive long trackId) {
        AuthenticatedPrincipal principal = requireActive(headers);
        interactions.addFavorite(principal.userId(), trackId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/favorites/{trackId}")
    public ResponseEntity<Void> removeFavorite(
            @RequestHeader HttpHeaders headers,
            @PathVariable @Positive long trackId) {
        AuthenticatedPrincipal principal = requireActive(headers);
        interactions.removeFavorite(principal.userId(), trackId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/favorites")
    public List<FavoriteSummary> favorites(@RequestHeader HttpHeaders headers) {
        return interactions.favorites(requireActive(headers).userId());
    }

    @PostMapping("/playlists")
    public ResponseEntity<PlaylistSummary> createPlaylist(
            @RequestHeader HttpHeaders headers,
            @Valid @RequestBody PlaylistRequest request) {
        AuthenticatedPrincipal principal = requireActive(headers);
        PlaylistSummary playlist = interactions.createPlaylist(
                principal.userId(), request.name(), request.description(), request.visibility());
        return ResponseEntity.status(HttpStatus.CREATED).body(playlist);
    }

    @GetMapping("/playlists")
    public List<PlaylistSummary> playlists(@RequestHeader HttpHeaders headers) {
        return interactions.playlists(requireActive(headers).userId());
    }

    @GetMapping("/playlists/{playlistId}")
    public PlaylistDetails playlist(
            @RequestHeader HttpHeaders headers,
            @PathVariable @Positive long playlistId) {
        return interactions.playlist(requireActive(headers).userId(), playlistId);
    }

    @PostMapping("/playlists/{playlistId}/tracks")
    public ResponseEntity<Void> addTrack(
            @RequestHeader HttpHeaders headers,
            @PathVariable @Positive long playlistId,
            @Valid @RequestBody TrackRequest request) {
        interactions.addTrack(requireActive(headers).userId(), playlistId, request.trackId(), request.sortOrder());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/playlists/{playlistId}/tracks/{trackId}")
    public ResponseEntity<Void> removeTrack(
            @RequestHeader HttpHeaders headers,
            @PathVariable @Positive long playlistId,
            @PathVariable @Positive long trackId) {
        interactions.removeTrack(requireActive(headers).userId(), playlistId, trackId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/history/{trackId}")
    public ResponseEntity<Void> updateHistory(
            @RequestHeader HttpHeaders headers,
            @PathVariable @Positive long trackId,
            @Valid @RequestBody HistoryRequest request) {
        interactions.updateHistory(requireActive(headers).userId(), trackId, request.positionMs());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/history")
    public List<ListeningHistorySummary> history(@RequestHeader HttpHeaders headers) {
        return interactions.history(requireActive(headers).userId());
    }

    @PostMapping("/admin/messages")
    public ResponseEntity<MessageSummary> publishMessage(
            @RequestHeader HttpHeaders headers,
            @Valid @RequestBody MessageRequest request) {
        AuthenticatedPrincipal principal = requireAdmin(headers);
        MessageSummary message = interactions.publishBroadcast(
                principal.userId(), principal.role(), request.subject(), request.body());
        return ResponseEntity.status(HttpStatus.CREATED).body(message);
    }

    @GetMapping("/messages")
    public List<MessageSummary> messages(@RequestHeader HttpHeaders headers) {
        return interactions.inbox(requireActive(headers).userId());
    }

    @PostMapping("/messages/{messageId}/read")
    public ResponseEntity<Void> markRead(
            @RequestHeader HttpHeaders headers,
            @PathVariable @Positive long messageId) {
        interactions.markRead(requireActive(headers).userId(), messageId);
        return ResponseEntity.noContent().build();
    }

    private static AuthenticatedPrincipal requireActive(HttpHeaders headers) {
        AuthenticatedPrincipal principal = TrustedIdentityHeaders.principalFrom(headers)
                .orElseThrow(InteractionAuthorizationException::new);
        if (principal.status() != AccountStatus.ACTIVE) {
            throw new InteractionAuthorizationException();
        }
        return principal;
    }

    private static AuthenticatedPrincipal requireAdmin(HttpHeaders headers) {
        AuthenticatedPrincipal principal = requireActive(headers);
        if (principal.role() != Role.ADMIN) {
            throw new InteractionAuthorizationException();
        }
        return principal;
    }

    public record PlaylistRequest(
            @NotBlank @Size(max = 120) String name,
            @Size(max = 2000) String description,
            @NotBlank @Size(max = 16) String visibility) {
    }

    public record TrackRequest(
            @Positive long trackId,
            @PositiveOrZero int sortOrder) {
    }

    public record HistoryRequest(@PositiveOrZero long positionMs) {
    }

    public record MessageRequest(
            @NotBlank @Size(max = 200) String subject,
            @NotBlank @NotNull String body) {
    }
}
