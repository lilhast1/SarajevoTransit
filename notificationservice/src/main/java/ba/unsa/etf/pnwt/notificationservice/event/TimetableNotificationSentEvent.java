package ba.unsa.etf.pnwt.notificationservice.event;

public record TimetableNotificationSentEvent(
        String sagaId,
        Long lineId,
        int notifiedCount
) {}
