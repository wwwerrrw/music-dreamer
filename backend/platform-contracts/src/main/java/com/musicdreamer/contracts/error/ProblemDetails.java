package com.musicdreamer.contracts.error;

import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;

public final class ProblemDetails {

    private ProblemDetails() {
    }

    public static ProblemDetail create(
            HttpStatusCode status,
            ApiErrorCode code,
            String detail,
            String traceId) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setProperty("code", code.name());
        problem.setProperty("traceId", traceId);
        return problem;
    }
}
