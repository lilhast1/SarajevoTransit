package com.sarajevotransit.userservice.dto;

public record LoyaltyBalanceResponse(
        Long userId,
        int currentBalance) {
}
