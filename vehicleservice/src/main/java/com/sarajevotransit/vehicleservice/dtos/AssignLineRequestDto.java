package com.sarajevotransit.vehicleservice.dtos;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class AssignLineRequestDto {
    private Long lineId;
    private String lineCode;
    private String lineName;
}
