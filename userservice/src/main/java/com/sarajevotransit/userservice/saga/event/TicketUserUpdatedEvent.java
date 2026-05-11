package com.sarajevotransit.userservice.saga.event;

public record TicketUserUpdatedEvent(
        String sagaId,
        int loyaltyPointsEarned
) {}
