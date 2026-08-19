package com.musicdreamer.identity;

import static org.assertj.core.api.Assertions.assertThat;

import com.musicdreamer.identity.application.ActivationApplicationService;
import com.musicdreamer.identity.application.RegistrationApplicationService;
import com.musicdreamer.identity.session.PersistentSessionManager;
import com.musicdreamer.identity.web.IdentityRegistrationController;
import com.musicdreamer.identity.web.IdentitySessionController;
import com.musicdreamer.security.session.SessionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

@SpringBootTest(
        classes = IdentityServiceApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
            "spring.datasource.url=jdbc:h2:mem:identity-application;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
            "spring.datasource.driver-class-name=org.h2.Driver",
            "spring.datasource.username=sa",
            "spring.datasource.password=",
            "spring.flyway.enabled=true",
            "spring.flyway.locations=classpath:db/migration/identity",
            "spring.flyway.baseline-on-migrate=false",
            "spring.cloud.nacos.discovery.enabled=false"
        })
class IdentityServiceApplicationTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private SessionRepository sessions;

    @Autowired
    private PersistentSessionManager sessionManager;

    @Autowired
    private RegistrationApplicationService registration;

    @Autowired
    private ActivationApplicationService activation;

    @Autowired
    private IdentityRegistrationController registrationController;

    @Autowired
    private IdentitySessionController sessionController;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void applicationWiresOwnedMigrationRedisSessionsHttpAndTransactions() {
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM users", Integer.class)).isZero();
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM flyway_schema_history WHERE version = '1' AND success = TRUE",
                        Integer.class))
                .isEqualTo(1);
        assertThat(sessions).isNotNull();
        assertThat(sessionManager).isNotNull();
        assertThat(registrationController).isNotNull();
        assertThat(sessionController).isNotNull();
        assertThat(transactionManager).isNotNull();
        assertThat(AopUtils.isAopProxy(registration)).isTrue();
        assertThat(AopUtils.isAopProxy(activation)).isTrue();
    }
}
