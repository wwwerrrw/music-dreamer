package com.musicdreamer.catalog;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration(proxyBeanMethods = false)
public class CatalogApplicationConfiguration {

    @Bean
    CatalogApplicationService catalogApplicationService(JdbcTemplate jdbc) {
        return new CatalogApplicationService(jdbc);
    }
}
