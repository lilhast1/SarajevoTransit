package com.sarajevotransit.userservice.service;

import com.sarajevotransit.userservice.dto.DiscoveryPingResponse;
import com.sarajevotransit.userservice.dto.FeedbackInstanceInfoResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class FeedbackDiscoveryService {

    private final RestClient.Builder restClientBuilder;
    private final LoadBalancerClient loadBalancerClient;

    @Value("${benchmark.feedback.direct-base-url:http://localhost:8091}")
    private String directBaseUrl;

    @Value("${benchmark.feedback.service-id:feedbackservice}")
    private String serviceId;

    @Value("${benchmark.feedback.instance-endpoint:/api/v1/discovery/instance}")
    private String instanceEndpoint;

    public DiscoveryPingResponse ping(String modeRaw) {
        String mode = normalizeMode(modeRaw);
        long start = System.nanoTime();

        FeedbackInstanceInfoResponse response = switch (mode) {
            case "direct" -> callDirect();
            case "lb" -> callThroughDiscovery();
            default -> throw new IllegalArgumentException("Unsupported mode: " + mode);
        };

        long durationMs = Duration.ofNanos(System.nanoTime() - start).toMillis();

        return new DiscoveryPingResponse(
                mode,
                response.serviceName(),
                response.instanceId(),
                response.host(),
                response.port(),
                response.timestamp(),
                durationMs);
    }

    private FeedbackInstanceInfoResponse callDirect() {
        String url = joinPath(trimTrailingSlash(directBaseUrl), instanceEndpoint);
        return call(url);
    }

    private FeedbackInstanceInfoResponse callThroughDiscovery() {
        ServiceInstance instance = loadBalancerClient.choose(serviceId);
        if (instance == null) {
            throw new IllegalStateException("No instances available for service: " + serviceId);
        }

        String baseUrl = instance.getUri().toString();
        String url = joinPath(trimTrailingSlash(baseUrl), instanceEndpoint);
        return call(url);
    }

    private FeedbackInstanceInfoResponse call(String url) {
        FeedbackInstanceInfoResponse response = restClientBuilder.build()
                .get()
                .uri(url)
                .retrieve()
                .body(FeedbackInstanceInfoResponse.class);

        if (response == null) {
            throw new IllegalStateException("Feedback service response is empty.");
        }

        return response;
    }

    private String normalizeMode(String modeRaw) {
        if (modeRaw == null || modeRaw.isBlank()) {
            return "lb";
        }

        String normalized = modeRaw.trim().toLowerCase();
        if (!"lb".equals(normalized) && !"direct".equals(normalized)) {
            throw new IllegalArgumentException("Mode must be either 'lb' or 'direct'.");
        }

        return normalized;
    }

    private String trimTrailingSlash(String value) {
        if (value.endsWith("/")) {
            return value.substring(0, value.length() - 1);
        }
        return value;
    }

    private String joinPath(String base, String path) {
        if (path == null || path.isBlank()) {
            return base;
        }
        if (path.startsWith("/")) {
            return base + path;
        }
        return base + "/" + path;
    }
}
