package com.sarajevotransit.vehicleservice.dtos;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.sarajevotransit.vehicleservice.model.enums.VehicleStatus;
import com.sarajevotransit.vehicleservice.model.enums.VehicleType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Value;

@Value
public class CreateVehicleRequestDto {
    @NotBlank
    String registrationNumber;
    String internalId;
    @NotNull
    VehicleType type;
    @NotNull
    Integer capacity;
    LocalDate manufactureDate;
    @NotNull
    VehicleStatus status;
    Double lastLat;
    Double lastLon;
    LocalDateTime lastGpsUpdate;
}
