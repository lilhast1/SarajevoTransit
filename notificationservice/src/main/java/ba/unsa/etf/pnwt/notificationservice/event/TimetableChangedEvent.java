package ba.unsa.etf.pnwt.notificationservice.event;

import java.time.Instant;

public record TimetableChangedEvent(
        String sagaId,
        Long lineId,
        String lineCode,
        String lineName,
        String changeType,
        Instant changedAt
) {}
