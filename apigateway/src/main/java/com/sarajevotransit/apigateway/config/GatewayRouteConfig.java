package com.sarajevotransit.apigateway.config;

import org.springframework.cloud.gateway.server.mvc.filter.LoadBalancerFilterFunctions;
import org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions;
import org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import static org.springframework.web.servlet.function.RequestPredicates.path;

@Configuration
public class GatewayRouteConfig {

    @Bean
    public RouterFunction<ServerResponse> authRoutes() {
        return GatewayRouterFunctions.route()
                .route(path("/api/auth/**"), HandlerFunctions.http())
                .filter(LoadBalancerFilterFunctions.lb("userservice"))
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> userServiceRoutes() {
        return GatewayRouterFunctions.route()
                .route(path("/api/v1/users/**").or(path("/api/users/**")),
                        HandlerFunctions.http())
                .filter(LoadBalancerFilterFunctions.lb("userservice"))
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> feedbackServiceRoutes() {
        return GatewayRouterFunctions.route()
                .route(path("/api/v1/workflows/**")
                        .or(path("/api/v1/reviews/**"))
                        .or(path("/api/v1/reports/**")),
                        HandlerFunctions.http())
                .filter(LoadBalancerFilterFunctions.lb("feedbackservice"))
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> notificationServiceRoutes() {
        return GatewayRouterFunctions.route()
                .route(path("/notifications/**").or(path("/subscriptions/**")),
                        HandlerFunctions.http())
                .filter(LoadBalancerFilterFunctions.lb("notifications"))
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> vehicleServiceRoutes() {
        return GatewayRouterFunctions.route()
                .route(path("/api/vehicles/**"),
                        HandlerFunctions.http())
                .filter(LoadBalancerFilterFunctions.lb("vehicle-service"))
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> routingServiceRoutes() {
        return GatewayRouterFunctions.route()
                .route(path("/api/v1/lines/**")
                        .or(path("/api/v1/stations/**"))
                        .or(path("/api/v1/directions/**"))
                        .or(path("/api/v1/timetables/**"))
                        .or(path("/api/v1/route-points/**"))
                        .or(path("/api/v1/direction-stations/**")),
                        HandlerFunctions.http())
                .filter(LoadBalancerFilterFunctions.lb("routingservice"))
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> moneymanRoutes() {
        return GatewayRouterFunctions.route()
                .route(path("/api/finance/**").or(path("/api/payments/**")),
                        HandlerFunctions.http())
                .filter(LoadBalancerFilterFunctions.lb("moneyman"))
                .build();
    }
}
