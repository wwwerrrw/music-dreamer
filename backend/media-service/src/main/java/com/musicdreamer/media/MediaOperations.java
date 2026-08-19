package com.musicdreamer.media;

public interface MediaOperations {

    MediaObject store(long ownerUserId, String filename, String mimeType, byte[] bytes, MediaType mediaType);

    MediaObject find(long id);

    MediaRange read(long id, long start, long end);
}
