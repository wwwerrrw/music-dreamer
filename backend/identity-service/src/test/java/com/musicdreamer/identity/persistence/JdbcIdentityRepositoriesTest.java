package com.musicdreamer.identity.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.musicdreamer.identity.application.IdentityFailure;
import com.musicdreamer.security.AccountStatus;
import com.musicdreamer.security.Role;
import java.time.Instant;
import org.flywaydb.core.Flyway;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class JdbcIdentityRepositoriesTest {

    @Test
    void repositoriesRoundTripOwnedIdentitySchemaWithOptimisticVersions() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:identity-repositories;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE");
        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration/identity")
                .load()
                .migrate();
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        JdbcUserAccounts users = new JdbcUserAccounts(jdbc);
        JdbcActivationTokens tokens = new JdbcActivationTokens(jdbc);

        var pending = users.createPending(
                "jdbc-user",
                "jdbc-user@example.test",
                "$2a$10$stored-bcrypt-hash");

        assertThat(pending.id()).isPositive();
        assertThat(pending.role()).isEqualTo(Role.USER);
        assertThat(pending.status()).isEqualTo(AccountStatus.PENDING);
        assertThat(users.findByLogin("jdbc-user@example.test")).contains(pending);
        assertThat(users.findAll()).containsExactly(pending);

        users.save(pending.activate());
        var active = users.findById(pending.id()).orElseThrow();
        assertThat(active.status()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(active.version()).isEqualTo(1L);

        Instant expiresAt = Instant.parse("2026-08-11T11:00:00Z");
        var createdToken = tokens.create(active.id(), "a".repeat(64), expiresAt);
        assertThat(createdToken.id()).isPositive();
        assertThat(tokens.findByDigest("a".repeat(64))).contains(createdToken);

        Instant usedAt = Instant.parse("2026-08-11T10:30:00Z");
        tokens.save(createdToken.markUsed(usedAt));
        var usedToken = tokens.findByDigest("a".repeat(64)).orElseThrow();
        assertThat(usedToken.usedAt()).isEqualTo(usedAt);
        assertThat(usedToken.version()).isEqualTo(1L);
    }

    @Test
    void uniqueConstraintViolationsAreTranslatedToRegistrationConflict() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:identity-conflicts;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE");
        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration/identity")
                .load()
                .migrate();
        JdbcUserAccounts users = new JdbcUserAccounts(new JdbcTemplate(dataSource));
        users.createPending(
                "existing-user",
                "existing@example.test",
                "$2a$10$stored-bcrypt-hash");

        assertRegistrationConflict(() -> users.createPending(
                "existing-user",
                "other@example.test",
                "$2a$10$other-bcrypt-hash"));
        assertRegistrationConflict(() -> users.createPending(
                "other-user",
                "existing@example.test",
                "$2a$10$other-bcrypt-hash"));
    }

    private static void assertRegistrationConflict(Runnable registration) {
        assertThatThrownBy(registration::run)
                .isInstanceOfSatisfying(IdentityFailure.class, failure ->
                        assertThat(failure.reason()).isEqualTo(IdentityFailure.Reason.REGISTRATION_CONFLICT));
    }
}
