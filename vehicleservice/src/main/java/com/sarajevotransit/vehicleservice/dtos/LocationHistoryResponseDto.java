package com.sarajevotransit.vehicleservice.dtos;

import java.time.LocalDateTime;

import lombok.Value;

@Value
public class LocationHistoryResponseDto {
    Long id;
    Long vehicleId;
    Double latitude;
    Double longitude;
    Double speed;
    LocalDateTime timestamp;
}
