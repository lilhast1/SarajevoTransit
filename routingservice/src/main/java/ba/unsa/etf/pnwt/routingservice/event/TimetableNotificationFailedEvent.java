package ba.unsa.etf.pnwt.routingservice.event;

public record TimetableNotificationFailedEvent(
        String sagaId,
        Long lineId,
        String reason
) {}
