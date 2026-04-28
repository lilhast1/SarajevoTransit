package com.sarajevotransit.vehicleservice.mappers;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import com.sarajevotransit.vehicleservice.model.Vehicle;
import com.sarajevotransit.vehicleservice.dtos.CreateVehicleRequestDto;
import com.sarajevotransit.vehicleservice.dtos.UpdateVehicleRequestDto;
import com.sarajevotransit.vehicleservice.dtos.VehicleResponseDTO;

@Mapper(componentModel = "spring")
public interface VehicleMapper {

    VehicleResponseDTO toResponse(Vehicle vehicle);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    Vehicle toEntity(CreateVehicleRequestDto dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    void updateEntityFromDto(UpdateVehicleRequestDto dto, @MappingTarget Vehicle vehicle);
}
