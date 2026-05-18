package ba.unsa.etf.pnwt.notificationservice.messaging;

import ba.unsa.etf.pnwt.notificationservice.config.RabbitMQConfig;
import ba.unsa.etf.pnwt.notificationservice.dto.BroadcastNotificationRequest;
import ba.unsa.etf.pnwt.notificationservice.dto.BroadcastNotificationResponse;
import ba.unsa.etf.pnwt.notificationservice.event.TimetableChangedEvent;
import ba.unsa.etf.pnwt.notificationservice.event.TimetableNotificationFailedEvent;
import ba.unsa.etf.pnwt.notificationservice.event.TimetableNotificationSentEvent;
import ba.unsa.etf.pnwt.notificationservice.model.NotificationType;
import ba.unsa.etf.pnwt.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TimetableChangedListener {

    private final NotificationService notificationService;
    private final TimetableNotificationPublisher publisher;

    @RabbitListener(queues = RabbitMQConfig.QUEUE_TIMETABLE_CHANGED)
    public void onTimetableChanged(TimetableChangedEvent event) {
        log.info("Saga [{}]: received timetable.changed for line {} ({})",
                event.sagaId(), event.lineId(), event.changeType());
        try {
            BroadcastNotificationRequest req = new BroadcastNotificationRequest();
            req.setLineId(event.lineId());
            req.setType(NotificationType.TIMETABLE_CHANGE);
            req.setTitle("Izmjena reda vožnje" + (event.lineCode() != null ? " — " + event.lineCode() : ""));
            req.setContent("Linija " + (event.lineName() != null ? event.lineName() : event.lineId()) + " ima izmjene u redu vožnje.");
            BroadcastNotificationResponse result = notificationService.broadcast(req);
            log.info("Saga [{}]: broadcast complete — {} notifications created", event.sagaId(), result.getNotificationsCreated());
            publisher.publishSent(new TimetableNotificationSentEvent(
                    event.sagaId(), event.lineId(), result.getNotificationsCreated()));
        } catch (Exception e) {
            log.error("Saga [{}]: broadcast failed for line {} — {}", event.sagaId(), event.lineId(), e.getMessage());
            publisher.publishFailed(new TimetableNotificationFailedEvent(
                    event.sagaId(), event.lineId(), e.getMessage()));
        }
    }
}
