package com.sarajevotransit.moneyman.saga.event;

public record TicketUserUpdatedEvent(
        String sagaId,
        int loyaltyPointsEarned
) {}
