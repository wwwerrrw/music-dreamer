package com.musicdreamer.identity.web;

import com.musicdreamer.identity.application.IdentityFailure;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public final class IdentityExceptionHandler {

    @ExceptionHandler(IdentityFailure.class)
    public ResponseEntity<ProblemDetail> handleIdentityFailure(
            IdentityFailure failure,
            HttpServletRequest request) {
        ProblemDetail problem = IdentityProblemDetails.from(failure, traceId(request));
        return ResponseEntity.status(problem.getStatus())
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    private static String traceId(HttpServletRequest request) {
        String requestId = request.getHeader("X-Request-Id");
        return requestId == null || requestId.isBlank()
                ? UUID.randomUUID().toString()
                : requestId;
    }
}
