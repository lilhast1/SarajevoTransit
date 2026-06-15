package com.sarajevotransit.vehicleservice.dtos;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
public class SetAvailabilityRequestDto {
    @NotNull
    private LocalDate date;
    private boolean available = true;
}
