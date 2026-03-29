package com.sarajevotransit.userservice.dto;

import com.sarajevotransit.userservice.model.LoyaltyTransactionType;

import java.time.LocalDateTime;

public record LoyaltyTransactionResponse(
        Long id,
        LoyaltyTransactionType transactionType,
        int points,
        String description,
        String referenceType,
        LocalDateTime createdAt) {
}
