package ba.unsa.etf.pnwt.routingservice.messaging;

import ba.unsa.etf.pnwt.routingservice.config.RabbitMQConfig;
import ba.unsa.etf.pnwt.routingservice.event.TimetableChangedEvent;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class TimetableEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public TimetableEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishTimetableChanged(TimetableChangedEvent event) {
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, RabbitMQConfig.ROUTING_TIMETABLE_CHANGED, event);
    }
}
