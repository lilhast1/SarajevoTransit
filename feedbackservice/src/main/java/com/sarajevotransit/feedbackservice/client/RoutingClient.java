package com.sarajevotransit.feedbackservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "routingservice", path = "/api/v1/lines")
public interface RoutingClient {
    @GetMapping("/{id}")
    Object getLineById(@PathVariable("id") Long id);
}