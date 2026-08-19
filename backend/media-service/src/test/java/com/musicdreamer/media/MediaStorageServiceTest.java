package com.musicdreamer.media;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.KeyHolder;

class MediaStorageServiceTest {

    @TempDir
    Path storageRoot;

    private JdbcTemplate jdbc;
    private MediaStorageService service;

    @BeforeEach
    void setUp() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:media-storage;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE");
        jdbc = new JdbcTemplate(dataSource);
        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration/media")
                .load()
                .migrate();
        service = new MediaStorageService(jdbc, storageRoot, new MediaFileValidator(10_000));
    }

    @Test
    void storesValidatedAudioAndReadsRequestedRange() throws Exception {
        byte[] bytes = new byte[] {'I', 'D', '3', 4, 10, 11, 12, 13};

        MediaObject object = service.store(41L, "demo.mp3", "audio/mpeg", bytes, MediaType.AUDIO);

        assertThat(object.id()).isPositive();
        assertThat(Files.readAllBytes(storageRoot.resolve(object.storageKey()))).containsExactly(bytes);
        assertThat(service.read(object.id(), 2, 5).bytes()).containsExactly(51, 4, 10, 11);
        assertThat(service.read(object.id(), 2, 5).totalLength()).isEqualTo(bytes.length);
    }

    @Test
    void storesAudioWhenMysqlLabelsTheGeneratedIdAsGeneratedKey() {
        JdbcTemplate mysqlLikeJdbc = new JdbcTemplate() {
            @Override
            public int update(PreparedStatementCreator creator, KeyHolder keys) {
                keys.getKeyList().add(Map.of("GENERATED_KEY", 77L));
                return 1;
            }
        };
        MediaStorageService mysqlLikeService = new MediaStorageService(
                mysqlLikeJdbc, storageRoot, new MediaFileValidator(10_000)) {
            @Override
            public MediaObject find(long id) {
                return new MediaObject(id, 41L, MediaType.AUDIO, "demo.mp3", "audio/demo.mp3",
                        4L, "audio/mpeg", "unused-in-this-test");
            }
        };

        MediaObject object = mysqlLikeService.store(
                41L, "demo.mp3", "audio/mpeg", new byte[] {'I', 'D', '3', 4}, MediaType.AUDIO);

        assertThat(object.id()).isEqualTo(77L);
    }
}
