package com.sarajevotransit.vehicleservice.mappers;

import com.sarajevotransit.vehicleservice.dtos.VehicleStatusRequestResponseDto;
import com.sarajevotransit.vehicleservice.model.VehicleStatusRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface VehicleStatusRequestMapper {

    @Mapping(target = "vehicleId", source = "vehicle.id")
    @Mapping(target = "vehicleRegistrationNumber", source = "vehicle.registrationNumber")
    VehicleStatusRequestResponseDto toResponse(VehicleStatusRequest request);

    List<VehicleStatusRequestResponseDto> toResponseList(List<VehicleStatusRequest> requests);
}
