package ba.unsa.etf.pnwt.notificationservice.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE = "ticket.saga.exchange";

    public static final String QUEUE_TICKET_COMPLETED  = "ticket-notification-completed-queue";
    public static final String QUEUE_TICKET_CANCELLED  = "ticket-notification-cancelled-queue";

    public static final String TIMETABLE_EXCHANGE            = "timetable.exchange";
    public static final String ROUTING_TIMETABLE_CHANGED     = "timetable.changed";
    public static final String ROUTING_TRIP_DELAY_CHANGED    = "trip.delay.changed";
    public static final String ROUTING_NOTIFICATION_SENT     = "timetable.notification.sent";
    public static final String ROUTING_NOTIFICATION_FAILED   = "timetable.notification.failed";
    public static final String QUEUE_TIMETABLE_CHANGED       = "timetable-notification-changed-queue";
    public static final String QUEUE_TRIP_DELAY_CHANGED      = "trip-delay-notification-changed-queue";

    @Bean
    public TopicExchange ticketSagaExchange() {
        return ExchangeBuilder.topicExchange(EXCHANGE).durable(true).build();
    }

    @Bean
    public TopicExchange timetableExchange() {
        return ExchangeBuilder.topicExchange(TIMETABLE_EXCHANGE).durable(true).build();
    }

    @Bean
    public Queue timetableChangedQueue() {
        return QueueBuilder.durable(QUEUE_TIMETABLE_CHANGED).build();
    }

    @Bean
    public Queue tripDelayChangedQueue() {
        return QueueBuilder.durable(QUEUE_TRIP_DELAY_CHANGED).build();
    }

    @Bean
    public Binding bindTimetableChanged(Queue timetableChangedQueue, TopicExchange timetableExchange) {
        return BindingBuilder.bind(timetableChangedQueue).to(timetableExchange).with(ROUTING_TIMETABLE_CHANGED);
    }

    @Bean
    public Binding bindTripDelayChanged(Queue tripDelayChangedQueue, TopicExchange timetableExchange) {
        return BindingBuilder.bind(tripDelayChangedQueue).to(timetableExchange).with(ROUTING_TRIP_DELAY_CHANGED);
    }

    @Bean
    public Queue ticketCompletedQueue() {
        return QueueBuilder.durable(QUEUE_TICKET_COMPLETED).build();
    }

    @Bean
    public Queue ticketCancelledQueue() {
        return QueueBuilder.durable(QUEUE_TICKET_CANCELLED).build();
    }

    @Bean
    public Binding bindPurchaseCompleted(Queue ticketCompletedQueue, TopicExchange ticketSagaExchange) {
        return BindingBuilder.bind(ticketCompletedQueue).to(ticketSagaExchange).with("ticket.purchase.completed");
    }

    @Bean
    public Binding bindPurchaseCancelled(Queue ticketCancelledQueue, TopicExchange ticketSagaExchange) {
        return BindingBuilder.bind(ticketCancelledQueue).to(ticketSagaExchange).with("ticket.purchase.cancelled");
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
