package com.musicdreamer.media;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class MediaFileValidator {

    private final long maxBytes;

    public MediaFileValidator(long maxBytes) {
        if (maxBytes <= 0) {
            throw new IllegalArgumentException("maxBytes must be positive");
        }
        this.maxBytes = maxBytes;
    }

    public ValidatedFile validate(String filename, String mimeType, byte[] bytes, MediaType mediaType) {
        if (filename == null || filename.isBlank() || filename.contains("/") || filename.contains("\\")
                || filename.contains("..")) {
            throw new MediaValidationException("Filename is invalid");
        }
        if (bytes == null || bytes.length == 0 || bytes.length > maxBytes) {
            throw new MediaValidationException("File size is invalid");
        }
        String extension = extension(filename);
        if (!matchesType(extension, mimeType, mediaType) || !matchesMagic(bytes, extension, mediaType)) {
            throw new MediaValidationException("File content does not match its declared type");
        }
        return new ValidatedFile(filename, mimeType, mediaType, bytes.length, digest(bytes));
    }

    private static String extension(String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot <= 0 || dot == filename.length() - 1) {
            throw new MediaValidationException("Filename extension is required");
        }
        return filename.substring(dot + 1).toLowerCase();
    }

    private static boolean matchesType(String extension, String mimeType, MediaType mediaType) {
        return switch (mediaType) {
            case AUDIO -> (extension.equals("mp3") && "audio/mpeg".equalsIgnoreCase(mimeType))
                    || (extension.equals("wav") && "audio/wav".equalsIgnoreCase(mimeType));
            case IMAGE -> (extension.equals("jpg") || extension.equals("jpeg"))
                    && "image/jpeg".equalsIgnoreCase(mimeType)
                    || extension.equals("png") && "image/png".equalsIgnoreCase(mimeType)
                    || extension.equals("webp") && "image/webp".equalsIgnoreCase(mimeType);
            case LYRICS -> extension.equals("lrc") && "text/plain".equalsIgnoreCase(mimeType);
        };
    }

    private static boolean matchesMagic(byte[] bytes, String extension, MediaType mediaType) {
        return switch (mediaType) {
            case AUDIO -> extension.equals("mp3")
                    ? (bytes.length >= 3 && bytes[0] == 'I' && bytes[1] == 'D' && bytes[2] == '3')
                            || (bytes.length >= 2 && (bytes[0] & 0xff) == 0xff && (bytes[1] & 0xe0) == 0xe0)
                    : bytes.length >= 12 && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                            && bytes[8] == 'W' && bytes[9] == 'A' && bytes[10] == 'V' && bytes[11] == 'E';
            case IMAGE -> extension.equals("jpg") || extension.equals("jpeg")
                    ? bytes.length >= 3 && (bytes[0] & 0xff) == 0xff && (bytes[1] & 0xff) == 0xd8
                    : extension.equals("png")
                            ? bytes.length >= 8 && (bytes[0] & 0xff) == 0x89 && bytes[1] == 'P' && bytes[2] == 'N'
                            && bytes[3] == 'G'
                            : bytes.length >= 12 && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                                    && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P';
            case LYRICS -> validUtf8(bytes);
        };
    }

    private static boolean validUtf8(byte[] bytes) {
        try {
            StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes));
            return true;
        } catch (CharacterCodingException invalidUtf8) {
            return false;
        }
    }

    private static String digest(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }

    public record ValidatedFile(
            String filename,
            String mimeType,
            MediaType mediaType,
            long byteSize,
            String sha256) {
    }
}
