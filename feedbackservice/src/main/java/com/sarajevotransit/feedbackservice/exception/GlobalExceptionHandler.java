package com.sarajevotransit.feedbackservice.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(NotFoundException exception, HttpServletRequest request) {
        return buildResponse(HttpStatus.NOT_FOUND, "not_found", exception.getMessage(), request.getRequestURI(),
                List.of());
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiErrorResponse> handleBadRequest(BadRequestException exception,
            HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, "bad_request", exception.getMessage(), request.getRequestURI(),
                List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationError(MethodArgumentNotValidException exception,
            HttpServletRequest request) {
        List<String> validationErrors = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::formatFieldError)
                .toList();

        return buildResponse(HttpStatus.BAD_REQUEST, "validation_error", "Validation failed",
                request.getRequestURI(), validationErrors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(ConstraintViolationException exception,
            HttpServletRequest request) {
        List<String> validationErrors = exception.getConstraintViolations()
                .stream()
                .map(violation -> {
                    String path = violation.getPropertyPath() == null
                            ? "request"
                            : violation.getPropertyPath().toString();
                    return extractLastPathSegment(path) + ": " + violation.getMessage();
                })
                .toList();

        return buildResponse(HttpStatus.BAD_REQUEST, "validation_error", "Validation failed",
                request.getRequestURI(), validationErrors);
    }

    @ExceptionHandler(ErrorResponseException.class)
    public ResponseEntity<ApiErrorResponse> handleErrorResponseException(ErrorResponseException exception,
            HttpServletRequest request) {
        HttpStatus status = HttpStatus.valueOf(exception.getStatusCode().value());
        String detail = exception.getBody() != null ? exception.getBody().getDetail() : null;
        String message = status.is5xxServerError()
                ? "Unexpected server error"
                : detail != null && !detail.isBlank()
                        ? exception.getBody().getDetail()
                        : status.getReasonPhrase();

        return buildResponse(status, resolveErrorCode(status), message, request.getRequestURI(), List.of());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleHttpMessageNotReadable(HttpMessageNotReadableException exception,
            HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, "malformed_json", "Malformed JSON request",
                request.getRequestURI(), List.of());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodArgumentTypeMismatch(
            MethodArgumentTypeMismatchException exception,
            HttpServletRequest request) {
        String message = "Invalid value for parameter '" + exception.getName() + "'.";
        return buildResponse(HttpStatus.BAD_REQUEST, "validation_error", "Validation failed", request.getRequestURI(),
                List.of(message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception exception, HttpServletRequest request) {
        log.error("Unhandled exception for path {}", request.getRequestURI(), exception);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "internal_error", "Unexpected server error",
                request.getRequestURI(), List.of());
    }

    private String formatFieldError(FieldError fieldError) {
        return fieldError.getField() + ": " + fieldError.getDefaultMessage();
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

    private ResponseEntity<ApiErrorResponse> buildResponse(HttpStatus status, String error, String message,
            String path, List<String> validationErrors) {
        ApiErrorResponse body = new ApiErrorResponse(
                LocalDateTime.now(),
                status.value(),
                error,
                message,
                path,
                validationErrors);
        return ResponseEntity.status(status).body(body);
    }

    private String extractLastPathSegment(String path) {
        int dotIndex = path.lastIndexOf('.');
        if (dotIndex >= 0 && dotIndex + 1 < path.length()) {
            return path.substring(dotIndex + 1);
        }
        return path;
    }
}
