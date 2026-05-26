package com.sarajevotransit.userservice.dto;

import com.sarajevotransit.userservice.model.LoyaltyTransactionType;
import com.sarajevotransit.userservice.model.LoyaltyCouponType;
import com.sarajevotransit.userservice.model.LoyaltyTier;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record LoyaltyTransactionResponse(
                Long id,
                LoyaltyTransactionType transactionType,
                int points,
                int pointsEarned,
                int pointsSpent,
                String description,
                String referenceType,
                Long transactionId,
                LocalDate expiryDate,
                String couponCode,
                LoyaltyCouponType couponType,
                LoyaltyTier couponTier,
                Integer couponDiscountPercent,
                String couponRideCode,
                LocalDateTime couponRedeemedAt,
                LocalDateTime createdAt) {
}
