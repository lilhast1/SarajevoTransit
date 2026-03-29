package com.sarajevotransit.userservice.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record AddLineReviewRequest(
                @NotBlank(message = "Line code is required") @Size(max = 40, message = "Line code can be at most 40 characters") String lineCode,
                @Min(value = 1, message = "Rating must be at least 1") @Max(value = 5, message = "Rating can be at most 5") int rating,
                @Size(max = 1500, message = "Review text can be at most 1500 characters") String reviewText,
                @NotNull(message = "Ride date is required") LocalDate rideDate) {
}
