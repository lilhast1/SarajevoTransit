package com.sarajevotransit.feedbackservice.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record VehicleSnapshot(
        Long id,
        String registrationNumber,
        String internalId,
        String type,
        String status) {
}
