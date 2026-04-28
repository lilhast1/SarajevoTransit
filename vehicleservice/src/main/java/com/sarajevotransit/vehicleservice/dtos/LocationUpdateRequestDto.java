package com.sarajevotransit.vehicleservice.dtos;

import jakarta.validation.constraints.NotNull;
import lombok.Value;

@Value
public class LocationUpdateRequestDto {
    @NotNull
    Double latitude;
    @NotNull
    Double longitude;
}
