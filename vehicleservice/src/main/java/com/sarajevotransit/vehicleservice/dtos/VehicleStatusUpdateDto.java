package com.sarajevotransit.vehicleservice.dtos;

import com.sarajevotransit.vehicleservice.model.enums.VehicleStatus;

import jakarta.validation.constraints.NotNull;
import lombok.Value;

@Value
public class VehicleStatusUpdateDto {
    @NotNull
    VehicleStatus status;
}
