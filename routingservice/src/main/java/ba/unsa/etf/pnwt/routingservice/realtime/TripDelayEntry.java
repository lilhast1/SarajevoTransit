package ba.unsa.etf.pnwt.routingservice.realtime;

import java.time.Instant;

public record TripDelayEntry(
        Integer timetableId,
        Integer directionId,
        String serviceDate,
        Integer delaySeconds,
        String reason,
        Long lineId,
        String lineCode,
        String lineName,
        Instant updatedAt
) {}
