package com.musicdreamer.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import com.musicdreamer.gateway.security.GatewaySecurityFilter;
import com.musicdreamer.gateway.security.PersistentSessionResolver;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.gateway.filter.ReactiveLoadBalancerClientFilter;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;

class GatewayServiceApplicationTest {

    @Test
    void applicationWiresRedisBackedSecurityAndIdentityRoute() {
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(GatewayServiceApplication.class)
                .web(WebApplicationType.REACTIVE)
                .run(
                        "--server.port=0",
                        "--spring.cloud.nacos.discovery.enabled=false")) {
            assertThat(context.getBean(GatewaySecurityFilter.class)).isNotNull();
            assertThat(context.getBean(PersistentSessionResolver.class)).isNotNull();
            assertThat(context.getBean(ReactiveStringRedisTemplate.class)).isNotNull();
            assertThat(context.getBean(ReactiveLoadBalancerClientFilter.class)).isNotNull();

            var routes = context.getBean(RouteDefinitionLocator.class)
                    .getRouteDefinitions()
                    .collectList()
                    .block();
            assertThat(routes)
                    .anySatisfy(route -> {
                        assertThat(route.getId()).isEqualTo("identity-service");
                        assertThat(route.getUri().toString()).isEqualTo("lb://identity-service");
                        assertThat(route.getPredicates())
                                .anySatisfy(predicate -> {
                                    assertThat(predicate.getName()).isEqualTo("Path");
                                    assertThat(predicate.getArgs()).containsValue("/api/identity/**");
                                });
                    });
            assertThat(routes)
                    .anySatisfy(route -> {
                        assertThat(route.getId()).isEqualTo("interaction-service");
                        assertThat(route.getUri().toString()).isEqualTo("lb://interaction-service");
                        assertThat(route.getPredicates())
                                .anySatisfy(predicate -> {
                                    assertThat(predicate.getName()).isEqualTo("Path");
                                    assertThat(predicate.getArgs()).containsValue("/api/interaction/**");
                                });
                    });
        }
    }
}
