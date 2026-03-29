package com.sarajevotransit.userservice.dto;

import java.time.LocalDateTime;

public record TravelHistoryResponse(
        Long id,
        String lineCode,
        String fromStop,
        String toStop,
        LocalDateTime traveledAt,
        int durationMinutes) {
}
