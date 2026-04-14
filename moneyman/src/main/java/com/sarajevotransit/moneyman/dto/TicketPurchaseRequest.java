package com.sarajevotransit.moneyman.dto;

import com.sarajevotransit.moneyman.model.enums.TicketType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TicketPurchaseRequest {
    @NotNull(message = "User ID is required")
    private Long userId;

    @NotNull(message = "Ticket Type is required")
    private TicketType ticketType;

    @NotNull(message = "Payment Method ID is required")
    private Long paymentMethodId;
}