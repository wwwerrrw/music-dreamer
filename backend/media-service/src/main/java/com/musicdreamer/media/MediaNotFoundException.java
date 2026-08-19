package com.musicdreamer.media;

public final class MediaNotFoundException extends RuntimeException {

    public MediaNotFoundException() {
        super("Media object was not found");
    }
}
