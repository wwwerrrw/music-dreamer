package com.musicdreamer.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.flywaydb.core.internal.database.DatabaseTypeRegister;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest(
        classes = CatalogServiceApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
            "spring.datasource.url=jdbc:h2:mem:catalog-application;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
            "spring.datasource.driver-class-name=org.h2.Driver",
            "spring.datasource.username=sa",
            "spring.datasource.password=",
            "spring.cloud.nacos.discovery.enabled=false"
        })
class CatalogServiceApplicationTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private CatalogOperations catalog;

    @Test
    void applicationWiresCatalogMigrationAndOperations() {
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM tracks", Integer.class)).isZero();
        assertThat(catalog).isInstanceOf(CatalogApplicationService.class);
    }

    @Test
    void productionClasspathSupportsMysqlFlywayMigrations() {
        assertThat(DatabaseTypeRegister.getDatabaseTypeForUrl(
                "jdbc:mysql://127.0.0.1:3306/music_dreamer_catalog")).isNotNull();
    }
}
