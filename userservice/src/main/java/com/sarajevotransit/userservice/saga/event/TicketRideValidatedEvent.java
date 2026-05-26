package com.sarajevotransit.userservice.saga.event;

import java.util.UUID;

public record TicketRideValidatedEvent(
        Long userId,
        UUID ticketId,
        String ticketType,
        int points
) {}