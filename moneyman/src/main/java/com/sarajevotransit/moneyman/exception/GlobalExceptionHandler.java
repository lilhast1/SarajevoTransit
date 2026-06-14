package com.sarajevotransit.moneyman.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.NOT_FOUND, "not_found", ex.getMessage(), request.getRequestURI(), List.of());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, "bad_request", ex.getMessage(), request.getRequestURI(), List.of());
    }

    @ExceptionHandler(PaymentFailedException.class)
    public ResponseEntity<ApiErrorResponse> handlePaymentFailed(PaymentFailedException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.PAYMENT_REQUIRED, "payment_failed", ex.getMessage(), request.getRequestURI(), List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<String> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .toList();
        return buildResponse(HttpStatus.BAD_REQUEST, "validation_error", "Validation failed", request.getRequestURI(), errors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
        List<String> errors = ex.getConstraintViolations().stream()
                .map(cv -> {
                    String path = cv.getPropertyPath().toString();
                    String field = path.contains(".") ? path.substring(path.lastIndexOf('.') + 1) : path;
                    return field + ": " + cv.getMessage();
                })
                .toList();
        return buildResponse(HttpStatus.BAD_REQUEST, "validation_error", "Validation failed", request.getRequestURI(), errors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleNotReadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, "malformed_json", "Malformed JSON request", request.getRequestURI(), List.of());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        String message = "Invalid value for parameter '" + ex.getName() + "'.";
        return buildResponse(HttpStatus.BAD_REQUEST, "validation_error", "Validation failed", request.getRequestURI(), List.of(message));
    }

    @ExceptionHandler(ErrorResponseException.class)
    public ResponseEntity<ApiErrorResponse> handleErrorResponse(ErrorResponseException ex, HttpServletRequest request) {
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        String detail = ex.getBody() != null ? ex.getBody().getDetail() : null;
        String message = status.is5xxServerError()
                ? "Unexpected server error"
                : (detail != null && !detail.isBlank() ? detail : status.getReasonPhrase());
        return buildResponse(status, resolveErrorCode(status), message, request.getRequestURI(), List.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneric(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception for path {}", request.getRequestURI(), ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "internal_error", "An unexpected error occurred", request.getRequestURI(), List.of());
    }

    private ResponseEntity<ApiErrorResponse> buildResponse(HttpStatus status, String error, String message, String path, List<String> validationErrors) {
        return ResponseEntity.status(status).body(
                new ApiErrorResponse(LocalDateTime.now(), status.value(), error, message, path, validationErrors));
    }

    private String resolveErrorCode(HttpStatus status) {
        return switch (status) {
            case BAD_REQUEST -> "bad_request";
            case NOT_FOUND -> "not_found";
            case CONFLICT -> "conflict";
            case FORBIDDEN -> "forbidden";
            case UNAUTHORIZED -> "unauthorized";
            case METHOD_NOT_ALLOWED -> "method_not_allowed";
            case UNSUPPORTED_MEDIA_TYPE -> "unsupported_media_type";
            default -> status.is5xxServerError() ? "internal_error" : "request_error";
        };
    }
}
