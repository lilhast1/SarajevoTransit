package com.sarajevotransit.userservice.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sarajevotransit.userservice.dto.TicketPurchaseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MoneymanClient {

    private final RestClient.Builder restClientBuilder;
    private final LoadBalancerClient loadBalancerClient;
    private final ObjectMapper objectMapper;

    @Value("${service.moneyman.id:moneyman}")
    private String moneymanServiceId;

    public List<TicketPurchaseResponse> getUserTickets(Long userId) {
        ServiceInstance instance = loadBalancerClient.choose(moneymanServiceId);
        if (instance == null) {
            // fallback to empty list if service is down, or we could throw exception
            return Collections.emptyList();
        }

        String url = instance.getUri().toString() + "/api/finance/wallet/" + userId + "?size=100";

        try {
            Map<String, Object> response = restClientBuilder.build()
                    .get()
                    .uri(url)
                    .header("X-User-Role", "ADMIN") // Access as ADMIN to fetch user's wallet
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});

            if (response != null && response.containsKey("content")) {
                return objectMapper.convertValue(
                        response.get("content"),
                        new TypeReference<List<TicketPurchaseResponse>>() {}
                );
            }
        } catch (Exception e) {
            // Log error and return empty list or handle accordingly
            System.err.println("Failed to fetch tickets from moneyman: " + e.getMessage());
        }

        return Collections.emptyList();
    }
}
