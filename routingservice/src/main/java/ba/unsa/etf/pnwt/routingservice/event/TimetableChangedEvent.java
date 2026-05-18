package ba.unsa.etf.pnwt.routingservice.event;

import java.time.Instant;

public record TimetableChangedEvent(
        String sagaId,
        Long lineId,
        String lineCode,
        String lineName,
        String changeType,
        Instant changedAt
) {}
