package ba.unsa.etf.pnwt.routingservice.messaging;

import ba.unsa.etf.pnwt.routingservice.config.RabbitMQConfig;
import ba.unsa.etf.pnwt.routingservice.event.TimetableChangedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TimetableEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishTimetableChanged(TimetableChangedEvent event) {
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, RabbitMQConfig.ROUTING_TIMETABLE_CHANGED, event);
    }
}
