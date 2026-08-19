package com.musicdreamer.media;

public final class MediaAuthorizationException extends RuntimeException {

    public MediaAuthorizationException() {
        super("Media operation is not permitted");
    }
}
