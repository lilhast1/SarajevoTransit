package com.sarajevotransit.feedbackservice.exception;

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

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(NotFoundException exception) {
        return buildResponse(HttpStatus.NOT_FOUND, exception.getMessage(), null);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiErrorResponse> handleBadRequest(BadRequestException exception) {
        return buildResponse(HttpStatus.BAD_REQUEST, exception.getMessage(), null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationError(MethodArgumentNotValidException exception) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return buildResponse(HttpStatus.BAD_REQUEST, "Validation failed", fieldErrors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(ConstraintViolationException exception) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        exception.getConstraintViolations().forEach(violation -> {
            String path = violation.getPropertyPath() == null ? "request" : violation.getPropertyPath().toString();
            String fieldName = extractLastPathSegment(path);
            fieldErrors.put(fieldName, violation.getMessage());
        });
        return buildResponse(HttpStatus.BAD_REQUEST, "Validation failed", fieldErrors);
    }

    @ExceptionHandler(ErrorResponseException.class)
    public ResponseEntity<ApiErrorResponse> handleErrorResponseException(ErrorResponseException exception) {
        HttpStatus status = HttpStatus.valueOf(exception.getStatusCode().value());
        String message = exception.getBody() != null && exception.getBody().getDetail() != null
                ? exception.getBody().getDetail()
                : exception.getMessage();
        return buildResponse(status, message, null);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleHttpMessageNotReadable(HttpMessageNotReadableException exception) {
        return buildResponse(HttpStatus.BAD_REQUEST, "Malformed JSON request", null);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodArgumentTypeMismatch(
            MethodArgumentTypeMismatchException exception) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        fieldErrors.put(exception.getName(), "Invalid value for parameter '" + exception.getName() + "'.");
        return buildResponse(HttpStatus.BAD_REQUEST, "Validation failed", fieldErrors);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception exception) {
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, exception.getMessage(), null);
    }

    private ResponseEntity<ApiErrorResponse> buildResponse(HttpStatus status, String message,
            Map<String, String> fieldErrors) {
        ApiErrorResponse body = new ApiErrorResponse(
                OffsetDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                fieldErrors);
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
