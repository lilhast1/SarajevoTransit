package com.sarajevotransit.userservice.saga.event;

public record TicketUserFailedEvent(
        String sagaId,
        String reason
) {}
