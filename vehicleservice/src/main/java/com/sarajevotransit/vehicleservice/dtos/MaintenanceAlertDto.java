package com.sarajevotransit.vehicleservice.dtos;

import com.sarajevotransit.vehicleservice.model.enums.VehicleStatus;
import com.sarajevotransit.vehicleservice.model.enums.VehicleType;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class MaintenanceAlertDto {
    private Long vehicleId;
    private String registrationNumber;
    private VehicleType type;
    private VehicleStatus status;
    private String assignedLineCode;
    private Integer serviceCycleIntervalDays;
    private LocalDateTime lastServiceEnd;
    private long daysOverdue;
}
