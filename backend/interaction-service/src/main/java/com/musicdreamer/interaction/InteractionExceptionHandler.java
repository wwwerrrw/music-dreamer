package com.musicdreamer.interaction;

import com.musicdreamer.contracts.error.ApiErrorCode;
import com.musicdreamer.contracts.error.ProblemDetails;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public final class InteractionExceptionHandler {

    @ExceptionHandler(InteractionAuthorizationException.class)
    ResponseEntity<ProblemDetail> forbidden(InteractionAuthorizationException failure, HttpServletRequest request) {
        return response(HttpStatus.FORBIDDEN, ApiErrorCode.INTERACTION_FORBIDDEN,
                "Interaction operation is not permitted", request);
    }

    @ExceptionHandler(InteractionNotFoundException.class)
    ResponseEntity<ProblemDetail> notFound(InteractionNotFoundException failure, HttpServletRequest request) {
        return response(HttpStatus.NOT_FOUND, ApiErrorCode.INTERACTION_NOT_FOUND, failure.getMessage(), request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ProblemDetail> invalid(IllegalArgumentException failure, HttpServletRequest request) {
        return response(HttpStatus.BAD_REQUEST, ApiErrorCode.INTERACTION_INVALID, failure.getMessage(), request);
    }

    private static ResponseEntity<ProblemDetail> response(
            HttpStatus status, ApiErrorCode code, String detail, HttpServletRequest request) {
        String traceId = request.getHeader("X-Request-Id");
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString();
        }
        ProblemDetail problem = ProblemDetails.create(status, code, detail, traceId);
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_PROBLEM_JSON).body(problem);
    }
}
