package ba.unsa.etf.pnwt.routingservice.messaging;

import ba.unsa.etf.pnwt.routingservice.config.RabbitMQConfig;
import ba.unsa.etf.pnwt.routingservice.event.TimetableChangedEvent;
import ba.unsa.etf.pnwt.routingservice.event.TimetableNotificationFailedEvent;
import ba.unsa.etf.pnwt.routingservice.event.TimetableNotificationSentEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class TimetableNotificationResultListener {

    private final TimetableEventPublisher publisher;

    @RabbitListener(queues = RabbitMQConfig.QUEUE_NOTIFICATION_SENT)
    public void onNotificationSent(TimetableNotificationSentEvent event) {
        log.info("Saga [{}]: notifications sent to {} subscribers for line {}",
                event.sagaId(), event.notifiedCount(), event.lineId());
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_NOTIFICATION_FAILED)
    public void onNotificationFailed(TimetableNotificationFailedEvent event) {
        log.warn("Saga [{}]: notification failed for line {} — {}. Retrying...",
                event.sagaId(), event.lineId(), event.reason());
        publisher.publishTimetableChanged(new TimetableChangedEvent(
                event.sagaId(),
                event.lineId(),
                null,
                null,
                "RETRY",
                Instant.now()
        ));
    }
}
