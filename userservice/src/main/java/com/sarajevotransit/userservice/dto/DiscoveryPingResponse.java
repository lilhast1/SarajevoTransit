package com.sarajevotransit.userservice.dto;

public record DiscoveryPingResponse(
        String mode,
        String serviceName,
        String instanceId,
        String host,
        int port,
        String servedAt,
        long durationMs) {
}
