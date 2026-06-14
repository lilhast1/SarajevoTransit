package com.sarajevotransit.vehicleservice.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CreateDriverServiceRequestDto {
    @NotNull
    private Long vehicleId;
    @NotBlank
    private String description;
}
