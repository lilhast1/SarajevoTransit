package com.sarajevotransit.vehicleservice.dtos;


import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
public class LocationUpdateRequestDto {
    @NotNull
    Double latitude;
    @NotNull
    Double longitude;
}
