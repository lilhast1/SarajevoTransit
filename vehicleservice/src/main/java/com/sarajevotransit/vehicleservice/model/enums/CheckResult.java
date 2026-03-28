package com.sarajevotransit.vehicleservice.model.enums;

public enum CheckResult {
    PASS,
    FAIL,
    WARNING // Passed, but needs attention soon (e.g., "Brake pads slightly worn")
}
