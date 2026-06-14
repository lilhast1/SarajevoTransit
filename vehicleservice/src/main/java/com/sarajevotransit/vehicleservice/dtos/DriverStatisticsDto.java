package com.sarajevotransit.vehicleservice.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class DriverStatisticsDto {
    private long totalAssignments;
    private long totalDaysActive;
    private List<String> lineCodesServed;
    private List<Long> vehicleIdsUsed;
}
