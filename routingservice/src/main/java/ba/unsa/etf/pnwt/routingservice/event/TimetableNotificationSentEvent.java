package ba.unsa.etf.pnwt.routingservice.event;

public record TimetableNotificationSentEvent(
        String sagaId,
        Long lineId,
        int notifiedCount
) {}
