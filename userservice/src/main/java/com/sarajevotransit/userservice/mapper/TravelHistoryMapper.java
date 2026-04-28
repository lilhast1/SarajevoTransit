package com.sarajevotransit.userservice.mapper;

import com.sarajevotransit.userservice.dto.TravelHistoryResponse;
import com.sarajevotransit.userservice.model.TravelHistoryEntry;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TravelHistoryMapper {

    private final ModelMapper modelMapper;

    public TravelHistoryResponse toResponse(TravelHistoryEntry entry) {
        if (entry == null) {
            return null;
        }

        return modelMapper.map(entry, TravelHistoryResponse.class);
    }
}
