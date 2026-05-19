package com.sarajevotransit.vehicleservice.dtos;

import com.sarajevotransit.vehicleservice.model.enums.RequestStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ResolveVehicleStatusRequestDto {
    @NotNull
    private RequestStatus resolution;   // APPROVED or REJECTED only
    @NotNull
    private Long resolvedByUserId;
    private String resolutionNote;
}
