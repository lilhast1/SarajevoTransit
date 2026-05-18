package ba.unsa.etf.pnwt.routingservice.messaging;

import ba.unsa.etf.pnwt.routingservice.config.RabbitMQConfig;
import ba.unsa.etf.pnwt.routingservice.event.TimetableChangedEvent;
import ba.unsa.etf.pnwt.routingservice.event.TimetableNotificationFailedEvent;
import ba.unsa.etf.pnwt.routingservice.event.TimetableNotificationSentEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class TimetableNotificationResultListener {

    private static final Logger log = LoggerFactory.getLogger(TimetableNotificationResultListener.class);

    private final TimetableEventPublisher publisher;

    public TimetableNotificationResultListener(TimetableEventPublisher publisher) {
        this.publisher = publisher;
    }

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
