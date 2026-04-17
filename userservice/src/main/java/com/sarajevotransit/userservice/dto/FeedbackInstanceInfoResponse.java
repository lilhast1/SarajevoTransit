package com.sarajevotransit.userservice.dto;

public record FeedbackInstanceInfoResponse(
        String serviceName,
        String instanceId,
        String host,
        int port,
        String timestamp) {
}
