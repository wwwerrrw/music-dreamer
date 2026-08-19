package com.musicdreamer.media;

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
public final class MediaExceptionHandler {

    @ExceptionHandler(MediaAuthorizationException.class)
    ResponseEntity<ProblemDetail> forbidden(MediaAuthorizationException failure, HttpServletRequest request) {
        return response(HttpStatus.FORBIDDEN, ApiErrorCode.MEDIA_FORBIDDEN, failure.getMessage(), request);
    }

    @ExceptionHandler(MediaValidationException.class)
    ResponseEntity<ProblemDetail> invalid(MediaValidationException failure, HttpServletRequest request) {
        return response(HttpStatus.BAD_REQUEST, ApiErrorCode.MEDIA_FILE_INVALID, failure.getMessage(), request);
    }

    @ExceptionHandler(MediaRangeException.class)
    ResponseEntity<ProblemDetail> invalidRange(MediaRangeException failure, HttpServletRequest request) {
        return response(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE,
                ApiErrorCode.MEDIA_RANGE_INVALID, failure.getMessage(), request);
    }

    @ExceptionHandler(EmptyResultDataAccessException.class)
    ResponseEntity<ProblemDetail> notFound(EmptyResultDataAccessException failure, HttpServletRequest request) {
        return response(HttpStatus.NOT_FOUND, ApiErrorCode.MEDIA_OBJECT_NOT_FOUND, "Media object was not found", request);
    }

    @ExceptionHandler(MediaNotFoundException.class)
    ResponseEntity<ProblemDetail> mediaNotFound(MediaNotFoundException failure, HttpServletRequest request) {
        return response(HttpStatus.NOT_FOUND, ApiErrorCode.MEDIA_OBJECT_NOT_FOUND, failure.getMessage(), request);
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
