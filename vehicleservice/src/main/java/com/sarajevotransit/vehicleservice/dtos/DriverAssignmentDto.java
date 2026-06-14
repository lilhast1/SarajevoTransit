package com.sarajevotransit.vehicleservice.dtos;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class DriverAssignmentDto {
    private Long id;
    private Long driverId;
    private Long vehicleId;
    private Long lineId;
    private String lineCode;
    private String lineName;
    private LocalDate assignmentDate;
    private boolean active;
    private LocalDateTime createdAt;
}
