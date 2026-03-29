package com.sarajevotransit.userservice.dto;

import com.sarajevotransit.userservice.model.LoyaltyTransactionType;

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
                LocalDateTime createdAt) {
}
