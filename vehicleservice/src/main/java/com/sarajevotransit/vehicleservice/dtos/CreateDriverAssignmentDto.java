package com.sarajevotransit.vehicleservice.dtos;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
public class CreateDriverAssignmentDto {
    @NotNull
    private Long driverId;
    @NotNull
    private Long vehicleId;
    private Long lineId;
    private String lineCode;
    private String lineName;
    @NotNull
    private LocalDate assignmentDate;
}
