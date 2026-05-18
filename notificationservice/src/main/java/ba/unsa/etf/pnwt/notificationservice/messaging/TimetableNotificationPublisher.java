package ba.unsa.etf.pnwt.notificationservice.messaging;

import ba.unsa.etf.pnwt.notificationservice.config.RabbitMQConfig;
import ba.unsa.etf.pnwt.notificationservice.event.TimetableNotificationFailedEvent;
import ba.unsa.etf.pnwt.notificationservice.event.TimetableNotificationSentEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TimetableNotificationPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishSent(TimetableNotificationSentEvent event) {
        rabbitTemplate.convertAndSend(RabbitMQConfig.TIMETABLE_EXCHANGE, RabbitMQConfig.ROUTING_NOTIFICATION_SENT, event);
    }

    public void publishFailed(TimetableNotificationFailedEvent event) {
        rabbitTemplate.convertAndSend(RabbitMQConfig.TIMETABLE_EXCHANGE, RabbitMQConfig.ROUTING_NOTIFICATION_FAILED, event);
    }
}
