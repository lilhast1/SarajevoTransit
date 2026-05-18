package com.sarajevotransit.feedbackservice.config;

import com.sarajevotransit.feedbackservice.client.NotificationServiceClient;
import com.sarajevotransit.feedbackservice.client.RoutingServiceClient;
import com.sarajevotransit.feedbackservice.client.UserServiceClient;
import com.sarajevotransit.feedbackservice.client.VehicleServiceClient;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class FeedbackServiceTestClientsConfig {

    @Bean
    @Primary
    public UserServiceClient userServiceClient() {
        return Mockito.mock(UserServiceClient.class);
    }

    @Bean
    @Primary
    public RoutingServiceClient routingServiceClient() {
        return Mockito.mock(RoutingServiceClient.class);
    }

    @Bean
    @Primary
    public VehicleServiceClient vehicleServiceClient() {
        return Mockito.mock(VehicleServiceClient.class);
    }

    @Bean
    @Primary
    public NotificationServiceClient notificationServiceClient() {
        return Mockito.mock(NotificationServiceClient.class);
    }
}