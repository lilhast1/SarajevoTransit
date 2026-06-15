package com.sarajevotransit.userservice.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE = "ticket.saga.exchange";

    public static final String ROUTING_USER_UPDATED = "ticket.user.updated";
    public static final String ROUTING_USER_FAILED = "ticket.user.failed";
    public static final String ROUTING_RIDE_VALIDATED = "ticket.ride.validated";

    public static final String QUEUE_USER_PURCHASE_INITIATED = "ticket-user-update-queue";
    public static final String QUEUE_USER_RIDE_VALIDATED = "ticket-user-ride-validated-queue";

    @Bean
    public TopicExchange ticketSagaExchange() {
        return ExchangeBuilder.topicExchange(EXCHANGE).durable(true).build();
    }

    @Bean
    public Queue userPurchaseInitiatedQueue() {
        return QueueBuilder.durable(QUEUE_USER_PURCHASE_INITIATED).build();
    }

    @Bean
    public Queue userRideValidatedQueue() {
        return QueueBuilder.durable(QUEUE_USER_RIDE_VALIDATED).build();
    }

    @Bean
    public Binding bindPurchaseInitiated(Queue userPurchaseInitiatedQueue, TopicExchange ticketSagaExchange) {
        return BindingBuilder.bind(userPurchaseInitiatedQueue).to(ticketSagaExchange).with("ticket.purchase.initiated");
    }

    @Bean
    public Binding bindRideValidated(Queue userRideValidatedQueue, TopicExchange ticketSagaExchange) {
        return BindingBuilder.bind(userRideValidatedQueue).to(ticketSagaExchange).with(ROUTING_RIDE_VALIDATED);
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
        converter.setAlwaysConvertToInferredType(true);
        return converter;
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }
}
