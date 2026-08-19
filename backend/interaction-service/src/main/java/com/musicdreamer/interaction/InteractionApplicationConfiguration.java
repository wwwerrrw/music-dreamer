package com.musicdreamer.interaction;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration(proxyBeanMethods = false)
@EnableTransactionManagement
public class InteractionApplicationConfiguration {

    @Bean
    InteractionApplicationService interactionApplicationService(JdbcTemplate jdbc) {
        return new InteractionApplicationService(jdbc);
    }
}
