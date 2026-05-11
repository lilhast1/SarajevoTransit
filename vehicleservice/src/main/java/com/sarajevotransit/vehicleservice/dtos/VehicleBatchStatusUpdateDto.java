package com.sarajevotransit.vehicleservice.dtos;


import java.util.List;


import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
public class VehicleBatchStatusUpdateDto {
    @NotEmpty
    @Valid
    List<VehicleStatusBatchItemDto> updates;
}