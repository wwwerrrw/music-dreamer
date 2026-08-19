package com.musicdreamer.gateway;

import com.musicdreamer.gateway.security.GatewayAuthenticationService;
import com.musicdreamer.gateway.security.GatewaySecurityFilter;
import com.musicdreamer.gateway.security.PersistentSessionResolver;
import com.musicdreamer.security.Sha256Tokens;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;

@Configuration(proxyBeanMethods = false)
public class GatewayApplicationConfiguration {

    @Bean
    PersistentSessionResolver sessionResolver(
            ReactiveStringRedisTemplate redis,
            @Value("${music-dreamer.gateway.redis-namespace:music-dreamer}") String namespace) {
        return new PersistentSessionResolver(redis, namespace, Sha256Tokens::digest);
    }

    @Bean
    GatewayAuthenticationService gatewayAuthenticationService(PersistentSessionResolver sessions) {
        return new GatewayAuthenticationService(sessions, Sha256Tokens::digest);
    }

    @Bean
    GatewaySecurityFilter gatewaySecurityFilter(GatewayAuthenticationService authentication) {
        return new GatewaySecurityFilter(authentication);
    }

}
