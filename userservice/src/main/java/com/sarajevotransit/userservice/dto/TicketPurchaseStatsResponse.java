package com.sarajevotransit.userservice.dto;

import com.sarajevotransit.userservice.model.TicketType;

import java.math.BigDecimal;

public record TicketPurchaseStatsResponse(
        TicketType ticketType,
        long purchaseCount,
        BigDecimal totalAmount) {
}
