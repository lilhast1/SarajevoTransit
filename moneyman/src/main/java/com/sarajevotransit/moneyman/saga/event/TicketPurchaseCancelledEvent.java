package com.sarajevotransit.moneyman.saga.event;

public record TicketPurchaseCancelledEvent(
        String sagaId,
        Long userId,
        String reason
) {}
