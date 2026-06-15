package com.sarajevotransit.vehicleservice.dtos;

import com.sarajevotransit.vehicleservice.model.enums.RequestStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ResolveDriverServiceRequestDto {
    @NotNull
    private RequestStatus status;
    private String resolutionNote;
}
