package com.sarajevotransit.vehicleservice.dtos;


import com.sarajevotransit.vehicleservice.model.enums.VehicleStatus;


import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class VehicleStatusUpdateDto {
    @NotNull
    VehicleStatus status;
}
