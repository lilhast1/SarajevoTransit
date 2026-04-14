package com.sarajevotransit.moneyman.dto;

import com.sarajevotransit.moneyman.model.enums.TicketType;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TicketPurchaseRequest {
    @NotNull(message = "User ID is required")
    private Long userId;

    @NotNull(message = "Ticket Type is required")
    private TicketType ticketType;

    public Long getUserId() {
        return userId;
    }

    public TicketType getTicketType() {
        return ticketType;
    }

    public Long getPaymentMethodId() {
        return paymentMethodId;
    }

    @NotNull(message = "Payment Method ID is required")
    private Long paymentMethodId;
}