package com.sarajevotransit.vehicleservice.dtos;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Value;

@Value
public class ServiceRecordResponseDto {
    Long id;
    Long vehicleId;
    LocalDateTime serviceStart;
    LocalDateTime serviceEnd;
    String description;
    BigDecimal cost;
    String partsChanged;
    long downtimeInHours; // computed field from getDowntimeInHours()
}