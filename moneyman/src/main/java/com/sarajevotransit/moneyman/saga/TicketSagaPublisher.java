package com.sarajevotransit.moneyman.saga;

import com.sarajevotransit.moneyman.config.RabbitMQConfig;
import com.sarajevotransit.moneyman.saga.event.*;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TicketSagaPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishPurchaseInitiated(TicketPurchaseInitiatedEvent event) {
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, RabbitMQConfig.ROUTING_PURCHASE_INITIATED, event);
    }

    public void publishPurchaseCompleted(TicketPurchaseCompletedEvent event) {
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, RabbitMQConfig.ROUTING_PURCHASE_COMPLETED, event);
    }

    public void publishPurchaseCancelled(TicketPurchaseCancelledEvent event) {
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, RabbitMQConfig.ROUTING_PURCHASE_CANCELLED, event);
    }
}
