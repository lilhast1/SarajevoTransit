package com.sarajevotransit.userservice.dto;

import java.time.LocalDateTime;

public record UserProfileResponse(
        Long id,
        String fullName,
        String email,
        int loyaltyPointsBalance,
        UserPreferenceResponse preference,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
