package com.musicdreamer.identity.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.musicdreamer.identity.application.IdentityFailure;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.ProblemDetail;

class IdentityProblemDetailsTest {

    @ParameterizedTest
    @CsvSource({
        "ACCOUNT_NOT_ACTIVE,403,IDENTITY_ACCOUNT_NOT_ACTIVE",
        "ACCOUNT_LOCKED,403,IDENTITY_ACCOUNT_LOCKED",
        "INVALID_CREDENTIALS,401,IDENTITY_INVALID_CREDENTIALS",
        "SESSION_INVALID,401,IDENTITY_SESSION_INVALID",
        "REGISTRATION_CONFLICT,409,IDENTITY_REGISTRATION_CONFLICT",
        "FORBIDDEN,403,IDENTITY_FORBIDDEN",
        "USER_NOT_FOUND,404,IDENTITY_USER_NOT_FOUND",
        "ACTIVATION_TOKEN_INVALID,400,IDENTITY_ACTIVATION_TOKEN_INVALID",
        "ACTIVATION_TOKEN_EXPIRED,410,IDENTITY_ACTIVATION_TOKEN_EXPIRED",
        "ACTIVATION_TOKEN_USED,409,IDENTITY_ACTIVATION_TOKEN_USED"
    })
    void mapsIdentityFailuresToStableProblemDetails(
            IdentityFailure.Reason reason,
            int expectedStatus,
            String expectedCode) {
        ProblemDetail problem = IdentityProblemDetails.from(
                new IdentityFailure(reason), "trace-identity-7");

        assertThat(problem.getStatus()).isEqualTo(expectedStatus);
        assertThat(problem.getProperties())
                .containsEntry("code", expectedCode)
                .containsEntry("traceId", "trace-identity-7");
        assertThat(problem.getDetail()).isNotBlank();
    }
}
