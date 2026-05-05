package com.sarajevotransit.feedbackservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "vehicleservice", path = "/api/vehicles")
public interface VehicleClient {
    @GetMapping("/{id}")
    Object getVehicleById(@PathVariable("id") Long id);
}