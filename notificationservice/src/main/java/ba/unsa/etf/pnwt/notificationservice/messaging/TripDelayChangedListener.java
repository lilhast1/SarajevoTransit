package ba.unsa.etf.pnwt.notificationservice.messaging;

import ba.unsa.etf.pnwt.notificationservice.config.RabbitMQConfig;
import ba.unsa.etf.pnwt.notificationservice.dto.BroadcastNotificationRequest;
import ba.unsa.etf.pnwt.notificationservice.event.TripDelayChangedEvent;
import ba.unsa.etf.pnwt.notificationservice.model.NotificationType;
import ba.unsa.etf.pnwt.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TripDelayChangedListener {

    private final NotificationService notificationService;

    @RabbitListener(queues = RabbitMQConfig.QUEUE_TRIP_DELAY_CHANGED)
    public void onTripDelayChanged(TripDelayChangedEvent event) {
        if (event.lineId() == null) {
            log.warn("Trip delay event {} skipped because lineId is null", event.eventId());
            return;
        }
        try {
            BroadcastNotificationRequest req = new BroadcastNotificationRequest();
            req.setLineId(event.lineId());
            req.setType(NotificationType.DELAY);

            String lineRef = event.lineCode() != null && !event.lineCode().isBlank()
                    ? event.lineCode()
                    : String.valueOf(event.lineId());

            if ("CLEAR".equalsIgnoreCase(event.action())) {
                req.setTitle("Kasnjenje uklonjeno - linija " + lineRef);
                req.setContent("Kasnjenje za liniju " + lineRef + " je uklonjeno za polazak "
                        + event.timetableId() + " (" + event.serviceDate() + ").");
            } else {
                int minutes = event.delaySeconds() == null ? 0 : Math.abs(event.delaySeconds()) / 60;
                String directionText = event.delaySeconds() != null && event.delaySeconds() < 0
                        ? "raniji polazak"
                        : "kasnjenje";
                req.setTitle("Trip update - linija " + lineRef);
                String reasonPart = event.reason() == null || event.reason().isBlank() ? "" : " Razlog: " + event.reason() + ".";
                req.setContent("Linija " + lineRef + " ima " + directionText + " od " + minutes
                        + " min za polazak " + event.timetableId() + " (" + event.serviceDate() + ")." + reasonPart);
            }

            int created = notificationService.broadcast(req).getNotificationsCreated();
            log.info("Trip delay event {} processed, notifications created={}", event.eventId(), created);
        } catch (Exception ex) {
            log.error("Trip delay event {} failed: {}", event.eventId(), ex.getMessage(), ex);
        }
    }
}
