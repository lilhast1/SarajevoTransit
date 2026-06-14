package com.sarajevotransit.moneyman.exception;

/**
 * Thrown when the payment gateway declines or fails to process a charge.
 * Mapped to HTTP 402 Payment Required by {@link GlobalExceptionHandler}.
 */
public class PaymentFailedException extends RuntimeException {
    public PaymentFailedException(String message) {
        super(message);
    }

    public PaymentFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
