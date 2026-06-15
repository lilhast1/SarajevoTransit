package com.sarajevotransit.vehicleservice.dtos;

import com.sarajevotransit.vehicleservice.model.enums.VehicleType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class CreateDriverProfileDto {
    @NotNull
    private Long userId;
    private String fullName;
    private String phone;
    private String licenseNumber;
    private List<VehicleType> licensedVehicleTypes;
}
