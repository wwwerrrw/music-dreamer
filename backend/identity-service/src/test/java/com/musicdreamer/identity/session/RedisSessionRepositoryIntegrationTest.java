package com.musicdreamer.identity.session;

import static org.assertj.core.api.Assertions.assertThat;

import com.musicdreamer.security.AccountStatus;
import com.musicdreamer.security.AuthenticatedPrincipal;
import com.musicdreamer.security.Role;
import com.musicdreamer.security.session.RedisSessionRepository;
import com.musicdreamer.security.session.SpringRedisSessionDataStore;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

@EnabledIfSystemProperty(named = "musicdreamer.redis.integration", matches = "true")
class RedisSessionRepositoryIntegrationTest {

    private static final String NAMESPACE = "music-dreamer:test:p1";
    private static final List<String> TEST_KEYS = List.of(
            NAMESPACE + ":session:digest-user-17-a",
            NAMESPACE + ":session:digest-user-17-b",
            NAMESPACE + ":session:digest-user-23",
            NAMESPACE + ":user-sessions:17",
            NAMESPACE + ":user-sessions:23");

    private LettuceConnectionFactory connectionFactory;
    private StringRedisTemplate redis;
    private RedisSessionRepository repository;

    @BeforeEach
    void connectAndClearExactTestKeys() {
        connectionFactory = new LettuceConnectionFactory(new RedisStandaloneConfiguration("127.0.0.1", 6380));
        connectionFactory.afterPropertiesSet();
        connectionFactory.start();
        redis = new StringRedisTemplate(connectionFactory);
        redis.afterPropertiesSet();
        redis.delete(TEST_KEYS);
        repository = new RedisSessionRepository(new SpringRedisSessionDataStore(redis), NAMESPACE);
    }

    @AfterEach
    void clearExactTestKeysAndDisconnect() {
        if (redis != null) {
            redis.delete(TEST_KEYS);
        }
        if (connectionFactory != null) {
            connectionFactory.destroy();
        }
    }

    @Test
    void realRedisPreservesTtlAndSessionRevocationBoundaries() {
        Duration ttl = Duration.ofMinutes(30);
        AuthenticatedPrincipal user =
                new AuthenticatedPrincipal(17L, Role.USER, AccountStatus.ACTIVE);
        AuthenticatedPrincipal singer =
                new AuthenticatedPrincipal(23L, Role.SINGER, AccountStatus.ACTIVE);
        repository.save("digest-user-17-a", user, ttl);
        repository.save("digest-user-17-b", user, ttl);
        repository.save("digest-user-23", singer, ttl);

        assertThat(repository.find("digest-user-17-a")).contains(user);
        assertThat(redis.getExpire(NAMESPACE + ":session:digest-user-17-a"))
                .isBetween(ttl.minusSeconds(5).toSeconds(), ttl.toSeconds());

        repository.delete("digest-user-17-a");
        assertThat(repository.find("digest-user-17-a")).isEmpty();
        assertThat(repository.find("digest-user-17-b")).contains(user);

        repository.deleteAllForUser(17L);
        assertThat(repository.find("digest-user-17-b")).isEmpty();
        assertThat(repository.find("digest-user-23")).contains(singer);
    }
}
