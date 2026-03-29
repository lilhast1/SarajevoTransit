package com.sarajevotransit.userservice.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record AddTravelHistoryRequest(
                @NotBlank(message = "Line code is required") @Size(max = 40, message = "Line code can be at most 40 characters") String lineCode,
                @NotBlank(message = "Departure stop is required") String fromStop,
                @NotBlank(message = "Arrival stop is required") String toStop,
                LocalDateTime traveledAt,
                @Min(value = 1, message = "Duration must be at least 1 minute") int durationMinutes) {
}
