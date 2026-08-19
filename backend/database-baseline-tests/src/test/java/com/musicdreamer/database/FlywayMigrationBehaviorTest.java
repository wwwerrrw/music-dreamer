package com.musicdreamer.database;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URL;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;

class FlywayMigrationBehaviorTest {

    @Test
    void identityMigrationCreatesOwnedEmptySchemaWithRequiredConstraints() throws Exception {
        assertMigration(new SchemaContract(
                "identity",
                Set.of("users", "email_activation_tokens"),
                Map.of(
                        "users", Set.of(Set.of("username"), Set.of("email")),
                        "email_activation_tokens", Set.of(Set.of("token_digest"))),
                Set.of(new ForeignKey("email_activation_tokens", "user_id", "users", "id"))));
    }

    @Test
    void catalogMigrationCreatesOwnedEmptySchemaWithoutCrossServiceForeignKeys() throws Exception {
        assertMigration(new SchemaContract(
                "catalog",
                Set.of("artist_profiles", "tracks", "tags", "track_tags", "outbox_events"),
                Map.of(
                        "artist_profiles", Set.of(Set.of("user_id")),
                        "tags", Set.of(Set.of("name")),
                        "track_tags", Set.of(Set.of("track_id", "tag_id")),
                        "outbox_events", Set.of(Set.of("event_id"))),
                Set.of(
                        new ForeignKey("track_tags", "track_id", "tracks", "id"),
                        new ForeignKey("track_tags", "tag_id", "tags", "id"))));
    }

    @Test
    void interactionMigrationCreatesOwnedEmptySchemaWithIdempotencyConstraints() throws Exception {
        assertMigration(new SchemaContract(
                "interaction",
                Set.of(
                        "track_favorites",
                        "playlists",
                        "playlist_tracks",
                        "playlist_follows",
                        "listening_history",
                        "messages",
                        "message_receipts",
                        "audit_events"),
                Map.of(
                        "track_favorites", Set.of(Set.of("user_id", "track_id")),
                        "playlist_tracks", Set.of(Set.of("playlist_id", "track_id")),
                        "playlist_follows", Set.of(Set.of("user_id", "playlist_id")),
                        "listening_history", Set.of(Set.of("user_id", "track_id")),
                        "message_receipts", Set.of(Set.of("message_id", "user_id")),
                        "audit_events", Set.of(Set.of("event_id"))),
                Set.of(
                        new ForeignKey("playlist_tracks", "playlist_id", "playlists", "id"),
                        new ForeignKey("playlist_follows", "playlist_id", "playlists", "id"),
                        new ForeignKey("message_receipts", "message_id", "messages", "id"))));
    }

    @Test
    void mediaMigrationCreatesOwnedEmptySchemaWithoutExternalForeignKeys() throws Exception {
        assertMigration(new SchemaContract(
                "media",
                Set.of("media_objects"),
                Map.of("media_objects", Set.of(Set.of("storage_key"))),
                Set.of()));
    }

    private static void assertMigration(SchemaContract contract) throws Exception {
        DataSource dataSource = isolatedDataSource(contract.owner());
        String migrationLocation = "db/migration/" + contract.owner();
        String migrationResource = migrationLocation + "/V1__baseline.sql";
        List<URL> packagedResources = Collections.list(
                Thread.currentThread().getContextClassLoader().getResources(migrationResource));

        assertThat(packagedResources)
                .as(contract.owner() + " migration artifact classpath resource")
                .hasSize(1);

        MigrateResult result = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:" + migrationLocation)
                .cleanDisabled(true)
                .load()
                .migrate();

        assertThat(result.migrationsExecuted).as("one versioned baseline migration").isEqualTo(1);

        try (Connection connection = dataSource.getConnection()) {
            assertThat(userTables(connection)).containsExactlyInAnyOrderElementsOf(contract.tables());

            for (String table : contract.tables()) {
                assertThat(primaryKeyColumns(connection, table)).as(table + " primary key").containsExactly("id");
                assertThat(columnNames(connection, table))
                        .as(table + " concurrency and timestamp columns")
                        .contains("version", "created_at", "updated_at");
                assertThat(rowCount(connection, table)).as(table + " must not seed credentials or business data").isZero();
            }

            for (Map.Entry<String, Set<Set<String>>> entry : contract.uniqueColumns().entrySet()) {
                assertThat(uniqueIndexes(connection, entry.getKey()))
                        .as(entry.getKey() + " unique relationships and natural keys")
                        .containsAll(entry.getValue());
            }

            assertThat(importedKeys(connection)).containsExactlyInAnyOrderElementsOf(contract.foreignKeys());
        }
    }

    private static DataSource isolatedDataSource(String owner) {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:" + owner
                + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE;DB_CLOSE_DELAY=-1");
        dataSource.setUser("sa");
        dataSource.setPassword("");
        return dataSource;
    }

    private static Set<String> userTables(Connection connection) throws SQLException {
        Set<String> tables = new HashSet<>();
        try (ResultSet rows = connection.getMetaData().getTables(null, null, "%", new String[] {"BASE TABLE"})) {
            while (rows.next()) {
                if ("public".equalsIgnoreCase(rows.getString("TABLE_SCHEM"))) {
                    String table = normalize(rows.getString("TABLE_NAME"));
                    if (!table.equals("flyway_schema_history")) {
                        tables.add(table);
                    }
                }
            }
        }
        return tables;
    }

    private static List<String> primaryKeyColumns(Connection connection, String table) throws SQLException {
        Map<Short, String> ordered = new HashMap<>();
        try (ResultSet rows = connection.getMetaData().getPrimaryKeys(null, null, table)) {
            while (rows.next()) {
                ordered.put(rows.getShort("KEY_SEQ"), normalize(rows.getString("COLUMN_NAME")));
            }
        }
        return ordered.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(Map.Entry::getValue)
                .toList();
    }

    private static Set<String> columnNames(Connection connection, String table) throws SQLException {
        Set<String> columns = new HashSet<>();
        try (ResultSet rows = connection.getMetaData().getColumns(null, null, table, "%")) {
            while (rows.next()) {
                columns.add(normalize(rows.getString("COLUMN_NAME")));
            }
        }
        return columns;
    }

    private static Set<Set<String>> uniqueIndexes(Connection connection, String table) throws SQLException {
        Map<String, List<IndexedColumn>> indexes = new HashMap<>();
        try (ResultSet rows = connection.getMetaData().getIndexInfo(null, null, table, true, false)) {
            while (rows.next()) {
                String indexName = rows.getString("INDEX_NAME");
                String columnName = rows.getString("COLUMN_NAME");
                if (indexName != null && columnName != null) {
                    indexes.computeIfAbsent(indexName, ignored -> new ArrayList<>())
                            .add(new IndexedColumn(rows.getShort("ORDINAL_POSITION"), normalize(columnName)));
                }
            }
        }

        Set<Set<String>> result = new HashSet<>();
        for (List<IndexedColumn> columns : indexes.values()) {
            result.add(new HashSet<>(columns.stream()
                    .sorted((left, right) -> Short.compare(left.position(), right.position()))
                    .map(IndexedColumn::name)
                    .toList()));
        }
        return result;
    }

    private static Set<ForeignKey> importedKeys(Connection connection) throws SQLException {
        Set<ForeignKey> keys = new HashSet<>();
        DatabaseMetaData metadata = connection.getMetaData();
        for (String table : userTables(connection)) {
            try (ResultSet rows = metadata.getImportedKeys(null, null, table)) {
                while (rows.next()) {
                    keys.add(new ForeignKey(
                            normalize(rows.getString("FKTABLE_NAME")),
                            normalize(rows.getString("FKCOLUMN_NAME")),
                            normalize(rows.getString("PKTABLE_NAME")),
                            normalize(rows.getString("PKCOLUMN_NAME"))));
                }
            }
        }
        return keys;
    }

    private static long rowCount(Connection connection, String table) throws SQLException {
        try (var statement = connection.createStatement();
                var rows = statement.executeQuery("SELECT COUNT(*) FROM " + table)) {
            rows.next();
            return rows.getLong(1);
        }
    }

    private static String normalize(String identifier) {
        return identifier.toLowerCase(java.util.Locale.ROOT);
    }

    private record SchemaContract(
            String owner,
            Set<String> tables,
            Map<String, Set<Set<String>>> uniqueColumns,
            Set<ForeignKey> foreignKeys) {}

    private record ForeignKey(String fromTable, String fromColumn, String toTable, String toColumn) {}

    private record IndexedColumn(short position, String name) {}
}
