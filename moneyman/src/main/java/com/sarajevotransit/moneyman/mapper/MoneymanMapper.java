package com.sarajevotransit.moneyman.mapper;

import com.sarajevotransit.moneyman.dto.TicketResponseDTO;
import com.sarajevotransit.moneyman.model.Ticket;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface MoneymanMapper {

    @Mapping(source = "transaction.amount", target = "amount")
    @Mapping(source = "transaction.externalTransactionId", target = "externalTransactionId")
    TicketResponseDTO toResponseDTO(Ticket ticket);
}