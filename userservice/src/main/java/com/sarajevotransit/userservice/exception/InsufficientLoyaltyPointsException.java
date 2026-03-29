package com.sarajevotransit.userservice.exception;

public class InsufficientLoyaltyPointsException extends RuntimeException {

    public InsufficientLoyaltyPointsException(String message) {
        super(message);
    }
}
