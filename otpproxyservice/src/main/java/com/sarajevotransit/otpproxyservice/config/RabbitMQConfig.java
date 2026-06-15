package com.sarajevotransit.otpproxyservice.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE = "timetable.exchange";
    public static final String QUEUE = "otp-rebuild-queue";
    public static final String ROUTING_KEY = "timetable.changed";

    @Bean
    public TopicExchange timetableExchange() {
        return new TopicExchange(EXCHANGE);
    }

    @Bean
    public Queue otpRebuildQueue() {
        return new Queue(QUEUE, true);
    }

    @Bean
    public Binding otpRebuildBinding(Queue otpRebuildQueue, TopicExchange timetableExchange) {
        return BindingBuilder.bind(otpRebuildQueue).to(timetableExchange).with(ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
