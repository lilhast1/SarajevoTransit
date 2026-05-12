package com.sarajevotransit.feedbackservice.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record UserProfileSnapshot(
        Long id,
        String fullName,
        String email) {
}
