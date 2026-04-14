package ba.unsa.etf.pnwt.notificationservice.exception;

import java.time.LocalDateTime;
import java.util.Map;

public class ApiErrorResponse {

    private LocalDateTime timestamp;
    private int status;
    private String error;
    private String message;
    private Map<String, String> validationErrors;

    public ApiErrorResponse(LocalDateTime timestamp, int status, String error, String message, Map<String, String> fieldErrors) {
        this.timestamp = timestamp;
        this.status = status;
        this.error = error;
        this.message = message;
        this.validationErrors = fieldErrors;
    }

    public LocalDateTime getTimestamp() { return timestamp; }
    public int getStatus() { return status; }
    public String getError() { return error; }
    public String getMessage() { return message; }
    public Map<String, String> getValidationErrors() { return validationErrors; }
}
