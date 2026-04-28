package com.sarajevotransit.vehicleservice.mappers;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.sarajevotransit.vehicleservice.dtos.CreateServiceRecordRequestDto;
import com.sarajevotransit.vehicleservice.dtos.ServiceRecordResponseDto;
import com.sarajevotransit.vehicleservice.model.ServiceRecord;

@Mapper(componentModel = "spring")
public interface ServiceRecordMapper {

    @Mapping(target = "vehicleId", source = "vehicle.id")
    @Mapping(target = "downtimeInHours", expression = "java(record.getDowntimeInHours())")
    ServiceRecordResponseDto toResponse(ServiceRecord record);

    List<ServiceRecordResponseDto> toResponseList(List<ServiceRecord> records);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "vehicle", ignore = true) // set manually in service
    ServiceRecord toEntity(CreateServiceRecordRequestDto dto);
}