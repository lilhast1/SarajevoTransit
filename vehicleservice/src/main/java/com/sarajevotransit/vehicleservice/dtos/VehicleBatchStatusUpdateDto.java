package com.sarajevotransit.vehicleservice.dtos;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Value;

@Value
public class VehicleBatchStatusUpdateDto {
    @NotEmpty
    @Valid
    List<VehicleStatusBatchItemDto> updates;
}