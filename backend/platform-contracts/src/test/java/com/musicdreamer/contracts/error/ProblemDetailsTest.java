package com.musicdreamer.contracts.error;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

class ProblemDetailsTest {

    @Test
    void exposesStableCodeAndTraceIdToApiClients() {
        ProblemDetail problem = ProblemDetails.create(
                HttpStatus.CONFLICT,
                ApiErrorCode.IDENTITY_EMAIL_CONFLICT,
                "Email already exists",
                "trace-123");

        assertThat(problem.getStatus()).isEqualTo(409);
        assertThat(problem.getDetail()).isEqualTo("Email already exists");
        assertThat(problem.getProperties())
                .containsEntry("code", "IDENTITY_EMAIL_CONFLICT")
                .containsEntry("traceId", "trace-123");
    }
}
