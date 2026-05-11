package com.sarajevotransit.userservice.saga;

import com.sarajevotransit.userservice.config.RabbitMQConfig;
import com.sarajevotransit.userservice.saga.event.TicketUserFailedEvent;
import com.sarajevotransit.userservice.saga.event.TicketUserUpdatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TicketSagaPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishUserUpdated(TicketUserUpdatedEvent event) {
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, RabbitMQConfig.ROUTING_USER_UPDATED, event);
    }

    public void publishUserFailed(TicketUserFailedEvent event) {
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, RabbitMQConfig.ROUTING_USER_FAILED, event);
    }
}
