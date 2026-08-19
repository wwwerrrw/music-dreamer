package com.musicdreamer.interaction;

public record PlaylistSummary(long id, long ownerUserId, String name, String description, String visibility) {
}
