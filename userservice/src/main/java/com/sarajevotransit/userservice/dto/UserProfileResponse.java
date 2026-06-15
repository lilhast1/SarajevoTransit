package com.sarajevotransit.userservice.dto;

import java.time.LocalDateTime;

public record UserProfileResponse(
                Long id,
                String fullName,
                String email,
                int loyaltyPointsBalance,
                int loyaltyPointsLifetime,
                String loyaltyTier,
                int loyaltyDiscountPercent,
                boolean loyaltyFreeRideEligible,
                UserPreferenceResponse preference,
                LocalDateTime createdAt,
                LocalDateTime updatedAt) {
}
