package com.sarajevotransit.userservice.dto;

import com.sarajevotransit.userservice.model.LoyaltyCouponType;
import com.sarajevotransit.userservice.model.LoyaltyTier;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record LoyaltyCouponResponse(
        String couponCode,
        LoyaltyCouponType couponType,
        LoyaltyTier tier,
        int discountPercent,
        String rideCode,
        int pointsSpent,
        boolean active,
        LocalDate expiresOn,
        LocalDateTime redeemedAt,
        LocalDateTime createdAt) {
}