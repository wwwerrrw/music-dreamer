package com.musicdreamer.catalog;

import java.util.List;

public record TrackPage(
        List<TrackSummary> items,
        int page,
        int size,
        long totalItems,
        int totalPages) {

    public TrackPage {
        items = List.copyOf(items);
    }
}
