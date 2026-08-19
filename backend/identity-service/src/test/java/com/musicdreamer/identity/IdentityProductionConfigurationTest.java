package com.musicdreamer.identity;

import static org.assertj.core.api.Assertions.assertThat;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

class IdentityProductionConfigurationTest {

    @Test
    void applicationAccountDoesNotRunSchemaMigrationsByDefault() {
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(
                        IdentityServiceApplication.class)
                .web(WebApplicationType.NONE)
                .run(
                        "--spring.datasource.url=jdbc:h2:mem:identity-production-default;MODE=MySQL;DB_CLOSE_DELAY=-1",
                        "--spring.datasource.driver-class-name=org.h2.Driver",
                        "--spring.datasource.username=sa",
                        "--spring.datasource.password=",
                        "--spring.cloud.nacos.discovery.enabled=false")) {
            assertThat(context.getBeansOfType(Flyway.class)).isEmpty();
        }
    }
}
