package com.sarajevotransit.moneyman.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE = "ticket.saga.exchange";

    public static final String ROUTING_PURCHASE_INITIATED = "ticket.purchase.initiated";
    public static final String ROUTING_USER_UPDATED       = "ticket.user.updated";
    public static final String ROUTING_USER_FAILED        = "ticket.user.failed";
    public static final String ROUTING_PURCHASE_COMPLETED = "ticket.purchase.completed";
    public static final String ROUTING_PURCHASE_CANCELLED = "ticket.purchase.cancelled";

    public static final String QUEUE_MONEYMAN_USER_UPDATED = "ticket-moneyman-user-updated-queue";
    public static final String QUEUE_MONEYMAN_USER_FAILED  = "ticket-moneyman-user-failed-queue";

    @Bean
    public TopicExchange ticketSagaExchange() {
        return ExchangeBuilder.topicExchange(EXCHANGE).durable(true).build();
    }

    @Bean
    public Queue moneymanUserUpdatedQueue() {
        return QueueBuilder.durable(QUEUE_MONEYMAN_USER_UPDATED).build();
    }

    @Bean
    public Queue moneymanUserFailedQueue() {
        return QueueBuilder.durable(QUEUE_MONEYMAN_USER_FAILED).build();
    }

    @Bean
    public Binding bindUserUpdated(Queue moneymanUserUpdatedQueue, TopicExchange ticketSagaExchange) {
        return BindingBuilder.bind(moneymanUserUpdatedQueue).to(ticketSagaExchange).with(ROUTING_USER_UPDATED);
    }

    @Bean
    public Binding bindUserFailed(Queue moneymanUserFailedQueue, TopicExchange ticketSagaExchange) {
        return BindingBuilder.bind(moneymanUserFailedQueue).to(ticketSagaExchange).with(ROUTING_USER_FAILED);
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
        // Infer Java type from the target @RabbitListener method parameter, not __TypeId__ header.
        // This avoids cross-service class-name mismatches when each service owns its own event class.
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
