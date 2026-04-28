package ba.unsa.etf.pnwt.notificationservice.dto;

public record DiscoveryPingResponse(
        String mode,
        String serviceName,
        String instanceId,
        String host,
        int port,
        String servedAt,
        long durationMs) {}
