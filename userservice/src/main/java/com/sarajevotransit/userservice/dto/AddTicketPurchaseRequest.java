package com.sarajevotransit.userservice.dto;

import com.sarajevotransit.userservice.model.TicketType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AddTicketPurchaseRequest(
                @NotNull(message = "Ticket type is required") TicketType ticketType,
                @NotNull(message = "Amount is required") @DecimalMin(value = "0.1", message = "Amount must be greater than 0") BigDecimal amount,
                @NotBlank(message = "Payment method is required") String paymentMethod,
                @NotBlank(message = "External transaction id is required") String externalTransactionId,
                @Size(max = 40, message = "Line code can be at most 40 characters") String lineCode,
                LocalDateTime purchasedAt) {
}
