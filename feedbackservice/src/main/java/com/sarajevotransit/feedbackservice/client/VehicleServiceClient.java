package com.sarajevotransit.feedbackservice.client;

import com.sarajevotransit.feedbackservice.client.dto.VehicleSnapshot;
import com.sarajevotransit.feedbackservice.exception.BadRequestException;
import com.sarajevotransit.feedbackservice.exception.NotFoundException;
import com.sarajevotransit.feedbackservice.exception.ServiceUnavailableException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
@RequiredArgsConstructor
public class VehicleServiceClient {

    private final RestClient.Builder restClientBuilder;
    private final LoadBalancerClient loadBalancerClient;

    @Value("${service.vehicles.id:vehicleservice}")
    private String vehicleServiceId;

    public VehicleSnapshot validateVehicle(Long vehicleId) {
        ServiceInstance instance = loadBalancerClient.choose(vehicleServiceId);
        if (instance == null) {
            throw new ServiceUnavailableException("Vehicle service is unavailable.");
        }

        String url = instance.getUri() + "/api/vehicles/" + vehicleId;
        try {
            VehicleSnapshot response = restClientBuilder.build()
                    .get()
                    .uri(url)
                    .retrieve()
                    .onStatus(status -> status.value() == 404,
                            (request, clientResponse) -> {
                                throw new NotFoundException("Vehicle with id " + vehicleId + " not found.");
                            })
                    .onStatus(status -> status.is5xxServerError(),
                            (request, clientResponse) -> {
                                throw new ServiceUnavailableException("Vehicle service is unavailable.");
                            })
                    .body(VehicleSnapshot.class);
            if (response == null) {
                throw new ServiceUnavailableException("Vehicle service returned an empty response.");
            }
            return response;
        } catch (RestClientException exception) {
            throw new ServiceUnavailableException("Vehicle service is unavailable.");
        }
    }
}
