package com.sarajevotransit.feedbackservice.dto;

public record ServiceInstanceInfoResponse(
        String serviceName,
        String instanceId,
        String host,
        int port,
        String timestamp) {
}
