package com.musicdreamer.identity.web;

import java.time.Duration;
import org.springframework.http.ResponseCookie;

final class MediaSessionCookie {

    static final String NAME = "music_dreamer_media_session";
    static final String PATH = "/api/media/objects";

    private MediaSessionCookie() {
    }

    static ResponseCookie issue(String token, Duration maxAge, boolean secure) {
        return base(token, secure)
                .maxAge(maxAge)
                .build();
    }

    static ResponseCookie expire(boolean secure) {
        return base("", secure)
                .maxAge(Duration.ZERO)
                .build();
    }

    private static ResponseCookie.ResponseCookieBuilder base(String value, boolean secure) {
        return ResponseCookie.from(NAME, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Strict")
                .path(PATH);
    }
}
