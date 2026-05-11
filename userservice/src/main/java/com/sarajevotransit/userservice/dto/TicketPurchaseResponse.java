package com.sarajevotransit.userservice.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record TicketPurchaseResponse(
        UUID id,
        String type,
        String status,
        LocalDateTime purchaseDate,
        LocalDateTime validUntil,
        String qrCodeData,
        BigDecimal amount,
        String externalTransactionId) {
}
