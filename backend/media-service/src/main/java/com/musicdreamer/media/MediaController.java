package com.musicdreamer.media;

import com.musicdreamer.security.AccountStatus;
import com.musicdreamer.security.AuthenticatedPrincipal;
import com.musicdreamer.security.Role;
import com.musicdreamer.security.TrustedIdentityHeaders;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/media/objects")
public final class MediaController {

    private static final Pattern RANGE = Pattern.compile("bytes=(\\d+)-(\\d*)");

    private final MediaOperations media;

    public MediaController(MediaOperations media) {
        this.media = media;
    }

    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<MediaObject> upload(
            @RequestHeader HttpHeaders headers,
            @RequestPart("file") MultipartFile file,
            @RequestParam MediaType mediaType) {
        AuthenticatedPrincipal principal = require(headers, Role.SINGER);
        try {
            MediaObject stored = media.store(
                    principal.userId(), file.getOriginalFilename(), file.getContentType(), file.getBytes(), mediaType);
            return ResponseEntity.status(HttpStatus.CREATED).body(stored);
        } catch (java.io.IOException failure) {
            throw new MediaValidationException("Unable to read uploaded file");
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<byte[]> download(
            @RequestHeader HttpHeaders headers,
            @PathVariable long id,
            @RequestHeader(value = HttpHeaders.RANGE, required = false) String rangeHeader) {
        requireActive(headers);
        MediaObject object = media.find(id);
        long start = 0;
        long end = object.byteSize() - 1;
        HttpStatus status = HttpStatus.OK;
        if (rangeHeader != null) {
            Matcher matcher = RANGE.matcher(rangeHeader.trim());
            if (!matcher.matches()) {
                throw new MediaRangeException("Requested range is invalid");
            }
            start = Long.parseLong(matcher.group(1));
            end = matcher.group(2).isEmpty()
                    ? object.byteSize() - 1
                    : Long.parseLong(matcher.group(2));
            status = HttpStatus.PARTIAL_CONTENT;
        }
        MediaRange content = media.read(id, start, end);
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.set(HttpHeaders.ACCEPT_RANGES, "bytes");
        responseHeaders.setContentLength(content.bytes().length);
        responseHeaders.set(HttpHeaders.CONTENT_TYPE, object.mimeType());
        if (status == HttpStatus.PARTIAL_CONTENT) {
            responseHeaders.set(HttpHeaders.CONTENT_RANGE,
                    "bytes " + content.start() + "-" + content.end() + "/" + content.totalLength());
        }
        return new ResponseEntity<>(content.bytes(), responseHeaders, status);
    }

    private static AuthenticatedPrincipal require(HttpHeaders headers, Role role) {
        AuthenticatedPrincipal principal = requireActive(headers);
        if (principal.role() != role) {
            throw new MediaAuthorizationException();
        }
        return principal;
    }

    private static AuthenticatedPrincipal requireActive(HttpHeaders headers) {
        AuthenticatedPrincipal principal = TrustedIdentityHeaders.principalFrom(headers)
                .orElseThrow(MediaAuthorizationException::new);
        if (principal.status() != AccountStatus.ACTIVE) {
            throw new MediaAuthorizationException();
        }
        return principal;
    }
}
