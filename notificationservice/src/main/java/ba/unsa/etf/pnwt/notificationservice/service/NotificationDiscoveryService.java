package ba.unsa.etf.pnwt.notificationservice.service;

import ba.unsa.etf.pnwt.notificationservice.dto.DiscoveryPingResponse;
import ba.unsa.etf.pnwt.notificationservice.dto.NotificationInstanceResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Service
public class NotificationDiscoveryService {

    private final RestClient.Builder restClientBuilder;
    private final LoadBalancerClient loadBalancerClient;

    @Value("${benchmark.notification.direct-base-url:http://localhost:8086}")
    private String directBaseUrl;

    @Value("${benchmark.notification.service-id:notifications}")
    private String serviceId;

    @Value("${benchmark.notification.instance-endpoint:/api/v1/discovery/instance}")
    private String instanceEndpoint;

    public NotificationDiscoveryService(RestClient.Builder restClientBuilder,
                                        LoadBalancerClient loadBalancerClient) {
        this.restClientBuilder = restClientBuilder;
        this.loadBalancerClient = loadBalancerClient;
    }

    public DiscoveryPingResponse ping(String modeRaw) {
        String mode = normalizeMode(modeRaw);
        long start = System.nanoTime();

        NotificationInstanceResponse response = switch (mode) {
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

    private NotificationInstanceResponse callDirect() {
        return call(trimTrailingSlash(directBaseUrl) + instanceEndpoint);
    }

    private NotificationInstanceResponse callThroughDiscovery() {
        ServiceInstance instance = loadBalancerClient.choose(serviceId);
        if (instance == null) {
            throw new IllegalStateException("No instances available for service: " + serviceId);
        }
        return call(trimTrailingSlash(instance.getUri().toString()) + instanceEndpoint);
    }

    private NotificationInstanceResponse call(String url) {
        NotificationInstanceResponse response = restClientBuilder.build()
                .get()
                .uri(url)
                .retrieve()
                .body(NotificationInstanceResponse.class);
        if (response == null) {
            throw new IllegalStateException("Empty response from: " + url);
        }
        return response;
    }

    private String normalizeMode(String raw) {
        if (raw == null || raw.isBlank()) return "lb";
        String normalized = raw.trim().toLowerCase();
        if (!"lb".equals(normalized) && !"direct".equals(normalized)) {
            throw new IllegalArgumentException("Mode must be 'lb' or 'direct'.");
        }
        return normalized;
    }

    private String trimTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
