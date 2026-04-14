package com.sarajevotransit.userservice.mapper;

import com.sarajevotransit.userservice.dto.TicketPurchaseResponse;
import com.sarajevotransit.userservice.model.TicketPurchaseHistoryEntry;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
public class TicketPurchaseMapper {

    private final ModelMapper modelMapper;

    public TicketPurchaseMapper(ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
    }

    public TicketPurchaseResponse toResponse(TicketPurchaseHistoryEntry entry) {
        if (entry == null) {
            return null;
        }

        return modelMapper.map(entry, TicketPurchaseResponse.class);
    }
}
