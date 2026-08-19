package com.musicdreamer.media;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class MediaFileValidatorTest {

    private final MediaFileValidator validator = new MediaFileValidator(10_000);

    @Test
    void acceptsMp3ByExtensionMimeAndMagic() {
        byte[] bytes = new byte[] {'I', 'D', '3', 4, 0, 0, 0, 0, 0, 0};

        MediaFileValidator.ValidatedFile result = validator.validate(
                "demo.mp3", "audio/mpeg", bytes, MediaType.AUDIO);

        assertThat(result.mediaType()).isEqualTo(MediaType.AUDIO);
        assertThat(result.sha256()).hasSize(64);
    }

    @Test
    void rejectsDisguisedAudioAndOversizedFiles() {
        assertThatThrownBy(() -> validator.validate(
                "demo.mp3", "audio/mpeg", "not-an-mp3".getBytes(StandardCharsets.UTF_8), MediaType.AUDIO))
                .isInstanceOf(MediaValidationException.class);

        MediaFileValidator limited = new MediaFileValidator(3);
        assertThatThrownBy(() -> limited.validate(
                "demo.mp3", "audio/mpeg", new byte[] {'I', 'D', '3', 4}, MediaType.AUDIO))
                .isInstanceOf(MediaValidationException.class);
    }

    @Test
    void rejectsPathTraversalAndUnsupportedExtensions() {
        assertThatThrownBy(() -> validator.validate(
                "..\\escape.mp3", "audio/mpeg", new byte[] {'I', 'D', '3'}, MediaType.AUDIO))
                .isInstanceOf(MediaValidationException.class);
        assertThatThrownBy(() -> validator.validate(
                "demo.exe", "application/octet-stream", new byte[] {0, 1}, MediaType.AUDIO))
                .isInstanceOf(MediaValidationException.class);
    }

    @Test
    void acceptsWavLrcAndPngMagic() {
        assertThat(validator.validate("demo.wav", "audio/wav",
                new byte[] {'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'A', 'V', 'E'}, MediaType.AUDIO)
                .mediaType()).isEqualTo(MediaType.AUDIO);
        assertThat(validator.validate("demo.lrc", "text/plain",
                "[00:01.00]hello".getBytes(StandardCharsets.UTF_8), MediaType.LYRICS)
                .mediaType()).isEqualTo(MediaType.LYRICS);
        assertThat(validator.validate("cover.png", "image/png",
                new byte[] {(byte) 0x89, 'P', 'N', 'G', 13, 10, 26, 10}, MediaType.IMAGE)
                .mediaType()).isEqualTo(MediaType.IMAGE);
    }
}
