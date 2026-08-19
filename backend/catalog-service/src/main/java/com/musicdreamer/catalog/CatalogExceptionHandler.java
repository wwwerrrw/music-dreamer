package com.musicdreamer.catalog;

import com.musicdreamer.contracts.error.ApiErrorCode;
import com.musicdreamer.contracts.error.ProblemDetails;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public final class CatalogExceptionHandler {

    @ExceptionHandler(CatalogAuthorizationException.class)
    ResponseEntity<ProblemDetail> forbidden(CatalogAuthorizationException failure, HttpServletRequest request) {
        return response(HttpStatus.FORBIDDEN, ApiErrorCode.CATALOG_FORBIDDEN, "Catalog operation is not permitted", request);
    }

    @ExceptionHandler(CatalogStateException.class)
    ResponseEntity<ProblemDetail> invalidState(CatalogStateException failure, HttpServletRequest request) {
        return response(HttpStatus.CONFLICT, ApiErrorCode.CATALOG_STATE_INVALID, failure.getMessage(), request);
    }

    @ExceptionHandler(EmptyResultDataAccessException.class)
    ResponseEntity<ProblemDetail> notFound(EmptyResultDataAccessException failure, HttpServletRequest request) {
        return response(HttpStatus.NOT_FOUND, ApiErrorCode.CATALOG_TRACK_NOT_FOUND, "Track was not found", request);
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
