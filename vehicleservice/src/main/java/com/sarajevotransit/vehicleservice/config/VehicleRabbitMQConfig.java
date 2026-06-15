package com.sarajevotransit.vehicleservice.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class VehicleRabbitMQConfig {

    public static final String VEHICLE_EXCHANGE           = "vehicle.exchange";
    public static final String ROUTING_MAINTENANCE        = "vehicle.maintenance.overdue";
    public static final String ROUTING_DRIVER_REQUEST     = "vehicle.driver.service.request";
    public static final String ROUTING_DRIVER_CONFLICT    = "vehicle.driver.assignment.conflict";
    public static final String ROUTING_MAINTENANCE_STATUS = "vehicle.maintenance.status.changed";

    @Bean
    public TopicExchange vehicleExchange() {
        return ExchangeBuilder.topicExchange(VEHICLE_EXCHANGE).durable(true).build();
    }

    @Bean
    public Jackson2JsonMessageConverter vehicleMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate vehicleRabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(vehicleMessageConverter());
        return template;
    }
}
