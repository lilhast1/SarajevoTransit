package com.sarajevotransit.moneyman.saga.event;

public record TicketUserFailedEvent(
        String sagaId,
        String reason
) {}
