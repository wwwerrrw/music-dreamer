package com.musicdreamer.identity.web;

import com.musicdreamer.contracts.error.ApiErrorCode;
import com.musicdreamer.contracts.error.ProblemDetails;
import com.musicdreamer.identity.application.IdentityFailure;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

public final class IdentityProblemDetails {

    private IdentityProblemDetails() {
    }

    public static ProblemDetail from(IdentityFailure failure, String traceId) {
        return switch (failure.reason()) {
            case ACCOUNT_NOT_ACTIVE -> ProblemDetails.create(
                    HttpStatus.FORBIDDEN,
                    ApiErrorCode.IDENTITY_ACCOUNT_NOT_ACTIVE,
                    "Account activation is required",
                    traceId);
            case ACCOUNT_LOCKED -> ProblemDetails.create(
                    HttpStatus.FORBIDDEN,
                    ApiErrorCode.IDENTITY_ACCOUNT_LOCKED,
                    "Account is locked",
                    traceId);
            case ACCOUNT_NOT_LOCKED -> ProblemDetails.create(
                    HttpStatus.CONFLICT,
                    ApiErrorCode.IDENTITY_ACCOUNT_NOT_LOCKED,
                    "Account is not locked",
                    traceId);
            case INVALID_CREDENTIALS -> ProblemDetails.create(
                    HttpStatus.UNAUTHORIZED,
                    ApiErrorCode.IDENTITY_INVALID_CREDENTIALS,
                    "Login credentials are invalid",
                    traceId);
            case SESSION_INVALID -> ProblemDetails.create(
                    HttpStatus.UNAUTHORIZED,
                    ApiErrorCode.IDENTITY_SESSION_INVALID,
                    "Session is missing or invalid",
                    traceId);
            case REGISTRATION_CONFLICT -> ProblemDetails.create(
                    HttpStatus.CONFLICT,
                    ApiErrorCode.IDENTITY_REGISTRATION_CONFLICT,
                    "Username or email is already registered",
                    traceId);
            case FORBIDDEN -> ProblemDetails.create(
                    HttpStatus.FORBIDDEN,
                    ApiErrorCode.IDENTITY_FORBIDDEN,
                    "Operation is not permitted",
                    traceId);
            case USER_NOT_FOUND -> ProblemDetails.create(
                    HttpStatus.NOT_FOUND,
                    ApiErrorCode.IDENTITY_USER_NOT_FOUND,
                    "User was not found",
                    traceId);
            case ROLE_CONFLICT -> ProblemDetails.create(
                    HttpStatus.CONFLICT,
                    ApiErrorCode.IDENTITY_ROLE_CONFLICT,
                    "Only regular users can be promoted to singer",
                    traceId);
            case ACTIVATION_TOKEN_INVALID -> ProblemDetails.create(
                    HttpStatus.BAD_REQUEST,
                    ApiErrorCode.IDENTITY_ACTIVATION_TOKEN_INVALID,
                    "Activation token is invalid",
                    traceId);
            case ACTIVATION_TOKEN_EXPIRED -> ProblemDetails.create(
                    HttpStatus.GONE,
                    ApiErrorCode.IDENTITY_ACTIVATION_TOKEN_EXPIRED,
                    "Activation token has expired",
                    traceId);
            case ACTIVATION_TOKEN_USED -> ProblemDetails.create(
                    HttpStatus.CONFLICT,
                    ApiErrorCode.IDENTITY_ACTIVATION_TOKEN_USED,
                    "Activation token has already been used",
                    traceId);
        };
    }
}
