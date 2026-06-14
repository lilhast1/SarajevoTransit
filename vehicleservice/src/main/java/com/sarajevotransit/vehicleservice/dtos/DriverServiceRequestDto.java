package com.sarajevotransit.vehicleservice.dtos;

import com.sarajevotransit.vehicleservice.model.enums.RequestStatus;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class DriverServiceRequestDto {
    private Long id;
    private Long driverId;
    private Long vehicleId;
    private String description;
    private RequestStatus status;
    private String resolutionNote;
    private Long resolvedByUserId;
    private LocalDateTime requestedAt;
    private LocalDateTime resolvedAt;
}
