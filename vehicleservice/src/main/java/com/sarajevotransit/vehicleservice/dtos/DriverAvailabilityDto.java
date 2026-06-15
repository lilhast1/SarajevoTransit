package com.sarajevotransit.vehicleservice.dtos;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
public class DriverAvailabilityDto {
    private Long id;
    private Long driverId;
    private LocalDate availableDate;
    private boolean available;
}
