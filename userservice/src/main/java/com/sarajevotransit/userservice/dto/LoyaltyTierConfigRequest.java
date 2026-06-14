package com.sarajevotransit.userservice.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record LoyaltyTierConfigRequest(
        @NotBlank String tierName,
        @Min(0) int minimumLifetimePoints,
        @Min(0) int discountPercent,
        boolean freeRideEligible,
        @Min(0) int couponCostDiscount,
        @Min(0) Integer couponCostFreeRide,
        @Min(0) int sortOrder) {
}
