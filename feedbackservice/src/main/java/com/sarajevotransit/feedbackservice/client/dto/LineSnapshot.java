package com.sarajevotransit.feedbackservice.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record LineSnapshot(
        Long id,
        String code,
        String name) {
}
