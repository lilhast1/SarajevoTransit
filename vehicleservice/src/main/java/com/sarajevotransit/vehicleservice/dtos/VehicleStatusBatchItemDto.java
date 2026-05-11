package com.sarajevotransit.vehicleservice.dtos;


import com.sarajevotransit.vehicleservice.model.enums.VehicleStatus;


import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
public class VehicleStatusBatchItemDto {
    @NotNull
    Long id;
    @NotNull
    VehicleStatus status;
}