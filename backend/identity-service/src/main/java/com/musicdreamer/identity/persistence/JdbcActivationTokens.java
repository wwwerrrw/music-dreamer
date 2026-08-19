package com.musicdreamer.identity.persistence;

import com.musicdreamer.identity.domain.ActivationToken;
import com.musicdreamer.identity.port.ActivationTokens;
import java.time.Instant;
import java.sql.Timestamp;
import java.util.Objects;
import java.util.Optional;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;

public final class JdbcActivationTokens implements ActivationTokens {

    private static final RowMapper<ActivationToken> TOKEN_ROW_MAPPER = (resultSet, rowNumber) -> {
        Timestamp usedAt = resultSet.getTimestamp("used_at");
        return new ActivationToken(
                resultSet.getLong("id"),
                resultSet.getLong("user_id"),
                resultSet.getString("token_digest"),
                resultSet.getTimestamp("expires_at").toInstant(),
                usedAt == null ? null : usedAt.toInstant(),
                resultSet.getLong("version"));
    };

    private final JdbcTemplate jdbc;

    public JdbcActivationTokens(JdbcTemplate jdbc) {
        this.jdbc = Objects.requireNonNull(jdbc, "jdbc");
    }

    @Override
    public Optional<ActivationToken> findByDigest(String digest) {
        return jdbc.query(
                        "SELECT id, user_id, token_digest, expires_at, used_at, version "
                                + "FROM email_activation_tokens WHERE token_digest = ?",
                        TOKEN_ROW_MAPPER,
                        digest)
                .stream()
                .findFirst();
    }

    @Override
    public void save(ActivationToken token) {
        int updated = jdbc.update(
                "UPDATE email_activation_tokens SET used_at = ?, version = ? WHERE id = ? AND version = ?",
                token.usedAt() == null ? null : Timestamp.from(token.usedAt()),
                token.version(),
                token.id(),
                token.version() - 1);
        if (updated != 1) {
            throw new OptimisticLockingFailureException("Activation token version changed: " + token.id());
        }
    }

    @Override
    public ActivationToken create(long userId, String digest, Instant expiresAt) {
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            var statement = connection.prepareStatement(
                    "INSERT INTO email_activation_tokens "
                            + "(user_id, token_digest, expires_at, version) VALUES (?, ?, ?, ?)",
                    new String[] {"id"});
            statement.setLong(1, userId);
            statement.setString(2, digest);
            statement.setTimestamp(3, Timestamp.from(expiresAt));
            statement.setLong(4, 0L);
            return statement;
        }, keyHolder);
        Number key = Objects.requireNonNull(keyHolder.getKey(), "generated activation token id");
        return new ActivationToken(key.longValue(), userId, digest, expiresAt, null, 0L);
    }
}
