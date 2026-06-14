package com.sarajevotransit.userservice.dto;

import java.time.LocalDateTime;

public record LoyaltyTierConfigResponse(
        Long id,
        String tierName,
        int minimumLifetimePoints,
        int discountPercent,
        boolean freeRideEligible,
        int couponCostDiscount,
        Integer couponCostFreeRide,
        int sortOrder,
        LocalDateTime updatedAt) {
}
