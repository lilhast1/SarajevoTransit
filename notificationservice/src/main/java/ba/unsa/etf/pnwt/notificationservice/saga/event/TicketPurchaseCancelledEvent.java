package ba.unsa.etf.pnwt.notificationservice.saga.event;

public record TicketPurchaseCancelledEvent(
        String sagaId,
        Long userId,
        String reason
) {}
