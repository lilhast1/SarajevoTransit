package ba.unsa.etf.pnwt.notificationservice.event;

public record TimetableNotificationFailedEvent(
        String sagaId,
        Long lineId,
        String reason
) {}
