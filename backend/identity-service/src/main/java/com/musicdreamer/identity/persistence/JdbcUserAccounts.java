package com.musicdreamer.identity.persistence;

import com.musicdreamer.identity.application.IdentityFailure;
import com.musicdreamer.identity.domain.UserAccount;
import com.musicdreamer.identity.port.UserAccounts;
import com.musicdreamer.security.AccountStatus;
import com.musicdreamer.security.Role;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;

public final class JdbcUserAccounts implements UserAccounts {

    private static final RowMapper<UserAccount> USER_ROW_MAPPER = (resultSet, rowNumber) -> new UserAccount(
            resultSet.getLong("id"),
            resultSet.getString("username"),
            resultSet.getString("email"),
            resultSet.getString("password_hash"),
            Role.valueOf(resultSet.getString("role")),
            AccountStatus.valueOf(resultSet.getString("account_status")),
            resultSet.getLong("version"));

    private static final String USER_COLUMNS =
            "id, username, email, password_hash, role, account_status, version";

    private final JdbcTemplate jdbc;

    public JdbcUserAccounts(JdbcTemplate jdbc) {
        this.jdbc = Objects.requireNonNull(jdbc, "jdbc");
    }

    @Override
    public Optional<UserAccount> findByLogin(String login) {
        return jdbc.query(
                        "SELECT " + USER_COLUMNS + " FROM users WHERE username = ? OR email = ? ORDER BY id LIMIT 1",
                        USER_ROW_MAPPER,
                        login,
                        login)
                .stream()
                .findFirst();
    }

    @Override
    public Optional<UserAccount> findById(long userId) {
        return jdbc.query(
                        "SELECT " + USER_COLUMNS + " FROM users WHERE id = ?",
                        USER_ROW_MAPPER,
                        userId)
                .stream()
                .findFirst();
    }

    @Override
    public List<UserAccount> findAll() {
        return jdbc.query("SELECT " + USER_COLUMNS + " FROM users ORDER BY id", USER_ROW_MAPPER);
    }

    @Override
    public void save(UserAccount account) {
        long expectedVersion = account.version() - 1;
        int updated = jdbc.update(
                "UPDATE users SET username = ?, email = ?, password_hash = ?, role = ?, "
                        + "account_status = ?, version = ? WHERE id = ? AND version = ?",
                account.username(),
                account.email(),
                account.passwordHash(),
                account.role().name(),
                account.status().name(),
                account.version(),
                account.id(),
                expectedVersion);
        if (updated != 1) {
            throw new OptimisticLockingFailureException("User version changed: " + account.id());
        }
    }

    @Override
    public UserAccount createPending(String username, String email, String passwordHash) {
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        try {
            jdbc.update(connection -> {
                var statement = connection.prepareStatement(
                        "INSERT INTO users "
                                + "(username, email, password_hash, role, account_status, display_name, version) "
                                + "VALUES (?, ?, ?, ?, ?, ?, ?)",
                        new String[] {"id"});
                statement.setString(1, username);
                statement.setString(2, email);
                statement.setString(3, passwordHash);
                statement.setString(4, Role.USER.name());
                statement.setString(5, AccountStatus.PENDING.name());
                statement.setString(6, username);
                statement.setLong(7, 0L);
                return statement;
            }, keyHolder);
        } catch (DuplicateKeyException failure) {
            throw new IdentityFailure(IdentityFailure.Reason.REGISTRATION_CONFLICT);
        }
        Number key = Objects.requireNonNull(keyHolder.getKey(), "generated user id");
        return findById(key.longValue()).orElseThrow();
    }
}
