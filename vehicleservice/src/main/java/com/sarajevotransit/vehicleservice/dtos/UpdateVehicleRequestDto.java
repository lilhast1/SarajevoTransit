package com.sarajevotransit.vehicleservice.dtos;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.sarajevotransit.vehicleservice.model.enums.VehicleStatus;
import com.sarajevotransit.vehicleservice.model.enums.VehicleType;

import lombok.Value;

@Value
public class UpdateVehicleRequestDto {
    String registrationNumber;
    String internalId;
    VehicleType type;
    Integer capacity;
    LocalDate manufactureDate;
    VehicleStatus status;
    Double lastLat;
    Double lastLon;
    LocalDateTime lastGpsUpdate;
}
