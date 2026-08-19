package com.musicdreamer.media;

public record MediaRange(byte[] bytes, long start, long end, long totalLength) {
}
