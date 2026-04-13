package com.sarajevotransit.userservice.exception;

import com.sarajevotransit.userservice.dto.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        return buildError(HttpStatus.NOT_FOUND, "not_found", ex.getMessage(), request.getRequestURI(), List.of());
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiErrorResponse> handleConflict(DuplicateResourceException ex, HttpServletRequest request) {
        return buildError(HttpStatus.CONFLICT, "conflict", ex.getMessage(), request.getRequestURI(), List.of());
    }

    @ExceptionHandler(InsufficientLoyaltyPointsException.class)
    public ResponseEntity<ApiErrorResponse> handleBadRequest(InsufficientLoyaltyPointsException ex,
            HttpServletRequest request) {
        return buildError(HttpStatus.BAD_REQUEST, "bad_request", ex.getMessage(), request.getRequestURI(), List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex,
            HttpServletRequest request) {
        List<String> validationErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::formatFieldError)
                .toList();

        return buildError(HttpStatus.BAD_REQUEST, "validation_error", "Validation failed", request.getRequestURI(),
                validationErrors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(ConstraintViolationException ex,
            HttpServletRequest request) {
        List<String> validationErrors = ex.getConstraintViolations()
                .stream()
                .map(violation -> {
                    String path = violation.getPropertyPath() == null
                            ? "request"
                            : violation.getPropertyPath().toString();
                    return extractLastPathSegment(path) + ": " + violation.getMessage();
                })
                .toList();

        return buildError(HttpStatus.BAD_REQUEST, "validation_error", "Validation failed", request.getRequestURI(),
                validationErrors);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException ex,
            HttpServletRequest request) {
        String message = "Invalid value for parameter '" + ex.getName() + "'.";
        return buildError(HttpStatus.BAD_REQUEST, "validation_error", "Validation failed", request.getRequestURI(),
                List.of(message));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleMalformedJson(HttpMessageNotReadableException ex,
            HttpServletRequest request) {
        return buildError(HttpStatus.BAD_REQUEST, "malformed_json", "Malformed JSON request", request.getRequestURI(),
                List.of());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(IllegalArgumentException ex,
            HttpServletRequest request) {
        return buildError(HttpStatus.BAD_REQUEST, "bad_request", ex.getMessage(), request.getRequestURI(), List.of());
    }

    @ExceptionHandler(ErrorResponseException.class)
    public ResponseEntity<ApiErrorResponse> handleErrorResponseException(ErrorResponseException ex,
            HttpServletRequest request) {
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        String detail = ex.getBody() != null ? ex.getBody().getDetail() : null;
        String safeMessage = status.is5xxServerError()
                ? "Unexpected server error"
                : (detail == null || detail.isBlank() ? status.getReasonPhrase() : detail);

        return buildError(status, resolveErrorCode(status), safeMessage, request.getRequestURI(), List.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnhandled(Exception ex, HttpServletRequest request) {
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR, "internal_error", "Unexpected server error",
                request.getRequestURI(), List.of());
    }

    private String formatFieldError(FieldError error) {
        return error.getField() + ": " + error.getDefaultMessage();
    }

    private String extractLastPathSegment(String path) {
        int dotIndex = path.lastIndexOf('.');
        if (dotIndex >= 0 && dotIndex + 1 < path.length()) {
            return path.substring(dotIndex + 1);
        }
        return path;
    }

    private String resolveErrorCode(HttpStatus status) {
        return switch (status) {
            case BAD_REQUEST -> "bad_request";
            case NOT_FOUND -> "not_found";
            case CONFLICT -> "conflict";
            case METHOD_NOT_ALLOWED -> "method_not_allowed";
            case UNSUPPORTED_MEDIA_TYPE -> "unsupported_media_type";
            default -> status.is5xxServerError() ? "internal_error" : "request_error";
        };
    }

    private ResponseEntity<ApiErrorResponse> buildError(
            HttpStatus status,
            String error,
            String message,
            String path,
            List<String> validationErrors) {
        ApiErrorResponse body = new ApiErrorResponse(
                LocalDateTime.now(),
                status.value(),
                error,
                message,
                path,
                validationErrors);
        return ResponseEntity.status(status).body(body);
    }
}
