package com.musicdreamer.interaction;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest(
        classes = InteractionServiceApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
            "spring.datasource.url=jdbc:h2:mem:interaction-application;MODE=MySQL;DATABASE_TO_LOWER=TRUE",
            "spring.datasource.driver-class-name=org.h2.Driver",
            "spring.datasource.username=sa",
            "spring.datasource.password=",
            "spring.cloud.nacos.discovery.enabled=false"
        })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class InteractionServiceApplicationTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private InteractionApplicationService interactions;

    @Test
    void applicationWiresInteractionMigrationAndOperations() {
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM track_favorites", Integer.class)).isZero();
        assertThat(interactions).isNotNull();
    }

    @Test
    void proxiedApplicationServiceKeepsFavoriteWritesIdempotent() {
        interactions.addFavorite(14L, 2L);
        interactions.addFavorite(14L, 2L);

        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM track_favorites WHERE user_id = 14 AND track_id = 2",
                Integer.class)).isEqualTo(1);
    }
}
