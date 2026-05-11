package com.sarajevotransit.vehicleservice.dtos;


import java.math.BigDecimal;
import java.time.LocalDateTime;


import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
public class CreateServiceRecordRequestDto {
    @NotNull
    LocalDateTime serviceStart;
    LocalDateTime serviceEnd; // nullable — record may still be open
    String description;
    BigDecimal cost;
    String partsChanged;
}