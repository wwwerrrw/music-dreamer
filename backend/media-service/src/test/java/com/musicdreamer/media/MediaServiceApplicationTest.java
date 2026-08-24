package com.musicdreamer.media;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.flywaydb.core.internal.database.DatabaseTypeRegister;
import org.springframework.aop.framework.Advised;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.interceptor.TransactionInterceptor;

@SpringBootTest(
        classes = MediaServiceApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
            "spring.datasource.url=jdbc:h2:mem:media-application;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
            "spring.datasource.driver-class-name=org.h2.Driver",
            "spring.datasource.username=sa",
            "spring.datasource.password=",
            "spring.cloud.nacos.discovery.enabled=false",
            "music-dreamer.media.storage-root=target/test-media-storage"
        })
class MediaServiceApplicationTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private MediaOperations media;

    @Test
    void applicationWiresMediaMigrationAndOperations() {
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM media_objects", Integer.class)).isZero();
        assertThat(media).isInstanceOf(MediaStorageService.class);
    }

    @Test
    void productionClasspathSupportsMysqlFlywayMigrations() {
        assertThat(DatabaseTypeRegister.getDatabaseTypeForUrl(
                "jdbc:mysql://127.0.0.1:3306/music_dreamer_media")).isNotNull();
    }

    @Test
    void uploadOperationsHaveNoSpringTransactionAdvice() {
        assertThat(media).satisfies(candidate -> {
            if (candidate instanceof Advised advised) {
                boolean hasTransactionAdvice = Arrays.stream(advised.getAdvisors())
                        .anyMatch(advisor -> advisor.getAdvice() instanceof TransactionInterceptor);
                assertThat(hasTransactionAdvice).isFalse();
            }
        });
    }
}
