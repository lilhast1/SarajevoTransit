package com.sarajevotransit.feedbackservice.client;

import com.sarajevotransit.feedbackservice.client.dto.LineSnapshot;
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
public class RoutingServiceClient {

    private final RestClient.Builder restClientBuilder;
    private final LoadBalancerClient loadBalancerClient;

    @Value("${service.routing.id:routingservice}")
    private String routingServiceId;

    public LineSnapshot validateLine(Long lineId) {
        ServiceInstance instance = loadBalancerClient.choose(routingServiceId);
        if (instance == null) {
            throw new ServiceUnavailableException("Routing service is unavailable.");
        }

        String url = instance.getUri() + "/api/v1/lines/" + lineId;
        try {
            LineSnapshot response = restClientBuilder.build()
                    .get()
                    .uri(url)
                    .retrieve()
                    .onStatus(status -> status.value() == 404,
                            (request, clientResponse) -> {
                                throw new NotFoundException("Line with id " + lineId + " not found.");
                            })
                    .onStatus(status -> status.is5xxServerError(),
                            (request, clientResponse) -> {
                                throw new ServiceUnavailableException("Routing service is unavailable.");
                            })
                    .body(LineSnapshot.class);
            if (response == null) {
                throw new ServiceUnavailableException("Routing service returned an empty response.");
            }
            return response;
        } catch (RestClientException exception) {
            throw new ServiceUnavailableException("Routing service is unavailable.");
        }
    }
}
