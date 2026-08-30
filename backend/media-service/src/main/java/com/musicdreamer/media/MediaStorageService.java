package com.musicdreamer.media;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

public class MediaStorageService implements MediaOperations {

    private final JdbcTemplate jdbc;
    private final Path storageRoot;
    private final MediaFileValidator validator;

    public MediaStorageService(JdbcTemplate jdbc, Path storageRoot, MediaFileValidator validator) {
        this.jdbc = Objects.requireNonNull(jdbc, "jdbc");
        this.storageRoot = Objects.requireNonNull(storageRoot, "storageRoot").toAbsolutePath().normalize();
        this.validator = Objects.requireNonNull(validator, "validator");
    }

    public MediaObject store(long ownerUserId, String filename, String mimeType, byte[] bytes, MediaType mediaType) {
        MediaFileValidator.ValidatedFile validated = validator.validate(filename, mimeType, bytes, mediaType);
        String extension = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
        String storageKey = mediaType.name().toLowerCase() + "/" + UUID.randomUUID() + "." + extension;
        Path destination = storageRoot.resolve(storageKey).normalize();
        if (!destination.startsWith(storageRoot)) {
            throw new MediaValidationException("Storage path is invalid");
        }
        try {
            Files.createDirectories(destination.getParent());
            Path temporary = Files.createTempFile(storageRoot, ".upload-", ".tmp");
            try {
                Files.write(temporary, bytes);
                try {
                    Files.move(temporary, destination, StandardCopyOption.ATOMIC_MOVE);
                } catch (java.nio.file.AtomicMoveNotSupportedException unsupported) {
                    Files.move(temporary, destination);
                }
            } finally {
                Files.deleteIfExists(temporary);
            }
        } catch (IOException failure) {
            throw new IllegalStateException("Media storage failed", failure);
        }

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            var statement = connection.prepareStatement(
                    "INSERT INTO media_objects (owner_user_id, media_type, original_filename, storage_key, "
                            + "byte_size, mime_type, sha256, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                    new String[] {"id"});
            statement.setLong(1, ownerUserId);
            statement.setString(2, mediaType.name());
            statement.setString(3, validated.filename());
            statement.setString(4, storageKey);
            statement.setLong(5, validated.byteSize());
            statement.setString(6, validated.mimeType());
            statement.setString(7, validated.sha256());
            statement.setString(8, "READY");
            return statement;
        }, keyHolder);
        Number generatedId = keyHolder.getKey();
        if (generatedId == null) {
            throw new IllegalStateException("Media insert did not return an id");
        }
        return find(generatedId.longValue());
    }

    public MediaObject find(long id) {
        try {
            return jdbc.queryForObject(
                    "SELECT id, owner_user_id, media_type, original_filename, storage_key, byte_size, mime_type, sha256 "
                            + "FROM media_objects WHERE id = ?",
                    (rs, rowNum) -> new MediaObject(
                            rs.getLong("id"),
                            rs.getLong("owner_user_id"),
                            MediaType.valueOf(rs.getString("media_type")),
                            rs.getString("original_filename"),
                            rs.getString("storage_key"),
                            rs.getLong("byte_size"),
                            rs.getString("mime_type"),
                            rs.getString("sha256")),
                    id);
        } catch (org.springframework.dao.EmptyResultDataAccessException missing) {
            throw new MediaNotFoundException();
        }
    }

    public MediaRange read(long id, long start, long end) {
        MediaObject object = find(id);
        Path source = storageRoot.resolve(object.storageKey()).normalize();
        if (!source.startsWith(storageRoot)) {
            throw new MediaValidationException("Storage path is invalid");
        }
        try (FileChannel channel = FileChannel.open(source, StandardOpenOption.READ)) {
            long totalLength = channel.size();
            if (start < 0 || start >= totalLength || end < start) {
                throw new MediaRangeException("Requested range is invalid");
            }
            long boundedEnd = Math.min(end, totalLength - 1);
            int length = (int) (boundedEnd - start + 1);
            ByteBuffer buffer = ByteBuffer.allocate(length);
            long position = start;
            while (buffer.hasRemaining()) {
                int read = channel.read(buffer, position);
                if (read < 0) {
                    break;
                }
                position += read;
            }
            buffer.flip();
            byte[] bytes = buffer.remaining() == length
                    ? buffer.array()
                    : Arrays.copyOf(buffer.array(), buffer.remaining());
            return new MediaRange(bytes, start, start + bytes.length - 1, totalLength);
        } catch (IOException failure) {
            throw new IllegalStateException("Media read failed", failure);
        }
    }
}
