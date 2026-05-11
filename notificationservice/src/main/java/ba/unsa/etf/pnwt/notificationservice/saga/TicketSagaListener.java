package ba.unsa.etf.pnwt.notificationservice.saga;

import ba.unsa.etf.pnwt.notificationservice.config.RabbitMQConfig;
import ba.unsa.etf.pnwt.notificationservice.dto.CreateNotificationRequest;
import ba.unsa.etf.pnwt.notificationservice.model.NotificationType;
import ba.unsa.etf.pnwt.notificationservice.saga.event.TicketPurchaseCancelledEvent;
import ba.unsa.etf.pnwt.notificationservice.saga.event.TicketPurchaseCompletedEvent;
import ba.unsa.etf.pnwt.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TicketSagaListener {

    private final NotificationService notificationService;

    @RabbitListener(queues = RabbitMQConfig.QUEUE_TICKET_COMPLETED)
    public void onTicketPurchaseCompleted(TicketPurchaseCompletedEvent event) {
        log.info("Saga [{}]: creating purchase confirmation notification for user {}", event.sagaId(), event.userId());
        CreateNotificationRequest req = new CreateNotificationRequest();
        req.setUserId(event.userId());
        req.setType(NotificationType.GENERAL);
        req.setTitle("Ticket activated!");
        req.setContent("Your " + event.ticketType().toLowerCase() + " ticket is now active. Enjoy your ride!");
        notificationService.create(req);
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_TICKET_CANCELLED)
    public void onTicketPurchaseCancelled(TicketPurchaseCancelledEvent event) {
        log.warn("Saga [{}]: creating purchase failure notification for user {}", event.sagaId(), event.userId());
        CreateNotificationRequest req = new CreateNotificationRequest();
        req.setUserId(event.userId());
        req.setType(NotificationType.GENERAL);
        req.setTitle("Purchase failed");
        req.setContent("Your ticket purchase could not be completed. Please try again.");
        notificationService.create(req);
    }
}
