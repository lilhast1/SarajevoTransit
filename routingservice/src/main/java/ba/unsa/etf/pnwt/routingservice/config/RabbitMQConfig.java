package ba.unsa.etf.pnwt.routingservice.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE = "timetable.exchange";

    public static final String ROUTING_TIMETABLE_CHANGED          = "timetable.changed";
    public static final String ROUTING_NOTIFICATION_SENT          = "timetable.notification.sent";
    public static final String ROUTING_NOTIFICATION_FAILED        = "timetable.notification.failed";
    public static final String ROUTING_TRIP_DELAY_CHANGED         = "trip.delay.changed";

    public static final String QUEUE_NOTIFICATION_SENT   = "timetable-routing-notification-sent-queue";
    public static final String QUEUE_NOTIFICATION_FAILED = "timetable-routing-notification-failed-queue";

    @Bean
    public TopicExchange timetableExchange() {
        return ExchangeBuilder.topicExchange(EXCHANGE).durable(true).build();
    }

    @Bean
    public Queue notificationSentQueue() {
        return QueueBuilder.durable(QUEUE_NOTIFICATION_SENT).build();
    }

    @Bean
    public Queue notificationFailedQueue() {
        return QueueBuilder.durable(QUEUE_NOTIFICATION_FAILED).build();
    }

    @Bean
    public Binding bindNotificationSent(Queue notificationSentQueue, TopicExchange timetableExchange) {
        return BindingBuilder.bind(notificationSentQueue).to(timetableExchange).with(ROUTING_NOTIFICATION_SENT);
    }

    @Bean
    public Binding bindNotificationFailed(Queue notificationFailedQueue, TopicExchange timetableExchange) {
        return BindingBuilder.bind(notificationFailedQueue).to(timetableExchange).with(ROUTING_NOTIFICATION_FAILED);
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
