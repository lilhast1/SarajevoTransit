package com.sarajevotransit.vehicleservice.mappers;

import java.util.List;

import org.mapstruct.Mapper;

import com.sarajevotransit.vehicleservice.dtos.LocationHistoryResponseDto;
import com.sarajevotransit.vehicleservice.model.VehicleLocationHistory;

@Mapper(componentModel = "spring")
public interface LocationHistoryMapper {

    LocationHistoryResponseDto toResponse(VehicleLocationHistory history);

    List<LocationHistoryResponseDto> toResponseList(List<VehicleLocationHistory> histories);
}
