package com.musicdreamer.media;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.musicdreamer.security.AccountStatus;
import com.musicdreamer.security.AuthenticatedPrincipal;
import com.musicdreamer.security.Role;
import com.musicdreamer.security.TrustedIdentityHeaders;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;

class MediaControllerTest {

    @Test
    void ordinaryUserCannotUpload() {
        MediaController controller = new MediaController(null);
        HttpHeaders headers = TrustedIdentityHeaders.forPrincipal(new HttpHeaders(),
                new AuthenticatedPrincipal(7L, Role.USER, AccountStatus.ACTIVE));

        assertThatThrownBy(() -> controller.upload(headers,
                new MockMultipartFile("file", "demo.mp3", "audio/mpeg", new byte[] {'I', 'D', '3'}),
                MediaType.AUDIO))
                .isInstanceOf(MediaAuthorizationException.class);
    }

    @Test
    void rangeResponseContains206AndContentRange() {
        MediaObject object = new MediaObject(1L, 7L, MediaType.AUDIO,
                "demo.mp3", "audio/demo.mp3", 8L, "audio/mpeg", "digest");
        MediaController controller = new MediaController(new MediaOperationsStub(object));
        HttpHeaders headers = TrustedIdentityHeaders.forPrincipal(new HttpHeaders(),
                new AuthenticatedPrincipal(7L, Role.USER, AccountStatus.ACTIVE));

        var response = controller.download(headers, 1L, "bytes=2-5");

        assertThat(response.getStatusCode().value()).isEqualTo(206);
        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_RANGE)).isEqualTo("bytes 2-5/8");
        assertThat(response.getBody()).containsExactly(2, 3, 4, 5);
    }

    @Test
    void browserOpenEndedRangeReturnsRemainingBytes() {
        MediaObject object = new MediaObject(1L, 7L, MediaType.AUDIO,
                "demo.mp3", "audio/demo.mp3", 8L, "audio/mpeg", "digest");
        MediaController controller = new MediaController(new MediaOperationsStub(object));
        HttpHeaders headers = TrustedIdentityHeaders.forPrincipal(new HttpHeaders(),
                new AuthenticatedPrincipal(7L, Role.USER, AccountStatus.ACTIVE));

        var response = controller.download(headers, 1L, "bytes=2-");

        assertThat(response.getStatusCode().value()).isEqualTo(206);
        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_RANGE)).isEqualTo("bytes 2-7/8");
        assertThat(response.getBody()).containsExactly(2, 3, 4, 5, 6, 7);
    }

    private static final class MediaOperationsStub implements MediaOperations {

        private final MediaObject object;

        private MediaOperationsStub(MediaObject object) {
            this.object = object;
        }

        @Override
        public MediaObject store(long ownerUserId, String filename, String mimeType, byte[] bytes, MediaType mediaType) {
            throw new UnsupportedOperationException();
        }

        @Override
        public MediaObject find(long id) {
            return object;
        }

        @Override
        public MediaRange read(long id, long start, long end) {
            int boundedEnd = (int) Math.min(end, object.byteSize() - 1);
            byte[] bytes = new byte[boundedEnd - (int) start + 1];
            for (int index = 0; index < bytes.length; index++) {
                bytes[index] = (byte) (start + index);
            }
            return new MediaRange(bytes, start, boundedEnd, object.byteSize());
        }
    }
}
