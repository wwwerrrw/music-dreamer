package com.musicdreamer.media;

public record MediaObject(
        long id,
        long ownerUserId,
        MediaType mediaType,
        String originalFilename,
        String storageKey,
        long byteSize,
        String mimeType,
        String sha256) {
}
