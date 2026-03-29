package com.sarajevotransit.userservice.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record LoyaltyEarnRequest(
        @Min(value = 1, message = "Points must be at least 1") int points,
        @NotBlank(message = "Description is required") String description,
        @NotBlank(message = "Reference type is required") String referenceType) {
}
