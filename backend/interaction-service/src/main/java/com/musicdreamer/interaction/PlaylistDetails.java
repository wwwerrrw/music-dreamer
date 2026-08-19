package com.musicdreamer.interaction;

import java.util.List;

public record PlaylistDetails(PlaylistSummary playlist, List<PlaylistTrackSummary> tracks) {
}
