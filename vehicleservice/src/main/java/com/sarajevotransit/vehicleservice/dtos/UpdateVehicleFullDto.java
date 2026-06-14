package com.sarajevotransit.vehicleservice.dtos;

import com.sarajevotransit.vehicleservice.model.enums.VehicleStatus;
import com.sarajevotransit.vehicleservice.model.enums.VehicleType;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
public class UpdateVehicleFullDto {
    private String registrationNumber;
    private String internalId;
    private VehicleType type;
    private Integer capacity;
    private LocalDate manufactureDate;
    private VehicleStatus status;
    private Integer serviceCycleIntervalDays;
}
