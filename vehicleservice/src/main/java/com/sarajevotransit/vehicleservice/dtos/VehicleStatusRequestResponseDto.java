package com.sarajevotransit.vehicleservice.dtos;

import com.sarajevotransit.vehicleservice.model.enums.RequestStatus;
import com.sarajevotransit.vehicleservice.model.enums.VehicleStatus;
import lombok.Value;

import java.time.LocalDateTime;

@Value
public class VehicleStatusRequestResponseDto {
    Long id;
    Long vehicleId;
    String vehicleRegistrationNumber;
    VehicleStatus requestedStatus;
    Long requestedByUserId;
    String notes;
    RequestStatus requestStatus;
    Long resolvedByUserId;
    String resolutionNote;
    LocalDateTime requestedAt;
    LocalDateTime resolvedAt;
}
