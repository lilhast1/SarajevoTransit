package com.sarajevotransit.userservice.dto;

import com.sarajevotransit.userservice.model.TicketType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TicketPurchaseResponse(
        Long id,
        TicketType ticketType,
        BigDecimal amount,
        String paymentMethod,
        String externalTransactionId,
        String lineCode,
        LocalDateTime purchasedAt) {
}
