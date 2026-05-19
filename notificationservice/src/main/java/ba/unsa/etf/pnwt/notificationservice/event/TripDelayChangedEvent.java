package ba.unsa.etf.pnwt.notificationservice.event;

import java.time.Instant;

public record TripDelayChangedEvent(
        String eventId,
        String action,
        Integer timetableId,
        String serviceDate,
        Integer delaySeconds,
        String reason,
        Long lineId,
        String lineCode,
        String lineName,
        Instant changedAt
) {}
