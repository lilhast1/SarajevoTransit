package com.sarajevotransit.userservice.saga.event;

import java.math.BigDecimal;
import java.util.UUID;

public record TicketPurchaseInitiatedEvent(
        String sagaId,
        Long userId,
        UUID ticketId,
        Long transactionId,
        String ticketType,
        BigDecimal amount,
        String externalTransactionId
) {}
