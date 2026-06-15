package com.sarajevotransit.vehicleservice.dtos;

import com.sarajevotransit.vehicleservice.model.enums.VehicleType;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
public class DriverProfileDto {
    private Long id;
    private Long userId;
    private String fullName;
    private String phone;
    private String licenseNumber;
    private List<VehicleType> licensedVehicleTypes;
    private LocalDateTime createdAt;
}
