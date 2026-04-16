package com.sarajevotransit.userservice.mapper;

import com.sarajevotransit.userservice.dto.LoyaltyTransactionResponse;
import com.sarajevotransit.userservice.model.LoyaltyTransaction;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LoyaltyTransactionMapper {

    private final ModelMapper modelMapper;

    public LoyaltyTransactionResponse toResponse(LoyaltyTransaction transaction) {
        if (transaction == null) {
            return null;
        }

        return modelMapper.map(transaction, LoyaltyTransactionResponse.class);
    }
}
