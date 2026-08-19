package com.musicdreamer.identity;

import com.musicdreamer.identity.application.AccountAdministrationService;
import com.musicdreamer.identity.application.ActivationApplicationService;
import com.musicdreamer.identity.application.IdentityApplicationService;
import com.musicdreamer.identity.application.LogoutApplicationService;
import com.musicdreamer.identity.application.RegistrationApplicationService;
import com.musicdreamer.identity.inbox.InMemoryDevelopmentInbox;
import com.musicdreamer.identity.persistence.JdbcActivationTokens;
import com.musicdreamer.identity.persistence.JdbcUserAccounts;
import com.musicdreamer.identity.security.BCryptCredentials;
import com.musicdreamer.identity.security.SecureRawTokenGenerator;
import com.musicdreamer.identity.security.Sha256TokenDigester;
import com.musicdreamer.identity.session.PersistentSessionManager;
import com.musicdreamer.security.session.RedisSessionRepository;
import com.musicdreamer.security.session.SessionRepository;
import com.musicdreamer.security.session.SpringRedisSessionDataStore;
import java.time.Clock;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration(proxyBeanMethods = false)
@EnableTransactionManagement
public class IdentityApplicationConfiguration {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    JdbcUserAccounts userAccounts(JdbcTemplate jdbc) {
        return new JdbcUserAccounts(jdbc);
    }

    @Bean
    JdbcActivationTokens activationTokens(JdbcTemplate jdbc) {
        return new JdbcActivationTokens(jdbc);
    }

    @Bean
    BCryptCredentials credentials(
            @Value("${music-dreamer.identity.bcrypt-strength:12}") int strength) {
        return new BCryptCredentials(strength);
    }

    @Bean
    SecureRawTokenGenerator rawTokenGenerator() {
        return new SecureRawTokenGenerator();
    }

    @Bean
    Sha256TokenDigester tokenDigester() {
        return new Sha256TokenDigester();
    }

    @Bean
    InMemoryDevelopmentInbox developmentInbox() {
        return new InMemoryDevelopmentInbox();
    }

    @Bean
    SessionRepository sessionRepository(
            StringRedisTemplate redis,
            @Value("${music-dreamer.identity.redis-namespace:music-dreamer}") String namespace) {
        return new RedisSessionRepository(new SpringRedisSessionDataStore(redis), namespace);
    }

    @Bean
    PersistentSessionManager sessionManager(
            SessionRepository sessions,
            SecureRawTokenGenerator rawTokenGenerator,
            Sha256TokenDigester tokenDigester,
            @Value("${music-dreamer.identity.session-lifetime:PT1H}") String sessionLifetime) {
        return new PersistentSessionManager(
                sessions,
                rawTokenGenerator,
                tokenDigester,
                Duration.parse(sessionLifetime));
    }

    @Bean
    RegistrationApplicationService registrationApplicationService(
            JdbcUserAccounts users,
            JdbcActivationTokens tokens,
            BCryptCredentials credentials,
            SecureRawTokenGenerator rawTokenGenerator,
            Sha256TokenDigester tokenDigester,
            InMemoryDevelopmentInbox inbox,
            Clock clock,
            @Value("${music-dreamer.identity.activation-lifetime:PT30M}") String activationLifetime) {
        return new RegistrationApplicationService(
                users,
                tokens,
                credentials,
                rawTokenGenerator,
                tokenDigester,
                inbox,
                clock,
                Duration.parse(activationLifetime));
    }

    @Bean
    ActivationApplicationService activationApplicationService(
            JdbcUserAccounts users,
            JdbcActivationTokens tokens,
            Sha256TokenDigester tokenDigester,
            Clock clock) {
        return new ActivationApplicationService(users, tokens, tokenDigester, clock);
    }

    @Bean
    IdentityApplicationService identityApplicationService(
            JdbcUserAccounts users,
            BCryptCredentials credentials,
            PersistentSessionManager sessions,
            Clock clock) {
        return new IdentityApplicationService(users, credentials, sessions, clock);
    }

    @Bean
    LogoutApplicationService logoutApplicationService(PersistentSessionManager sessions) {
        return new LogoutApplicationService(sessions);
    }

    @Bean
    AccountAdministrationService accountAdministrationService(
            JdbcUserAccounts users,
            PersistentSessionManager sessions) {
        return new AccountAdministrationService(users, sessions);
    }
}
