package ba.unsa.etf.pnwt.notificationservice.saga.event;

import java.util.UUID;

public record TicketPurchaseCompletedEvent(
        String sagaId,
        Long userId,
        UUID ticketId,
        String ticketType
) {}
