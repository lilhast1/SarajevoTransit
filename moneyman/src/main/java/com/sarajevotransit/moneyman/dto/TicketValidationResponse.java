package com.sarajevotransit.moneyman.dto;

import com.sarajevotransit.moneyman.model.enums.TicketStatus;
import com.sarajevotransit.moneyman.model.enums.TicketType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TicketValidationResponse {
    private boolean valid;
    private UUID ticketId;
    private TicketType type;
    private TicketStatus status;
    private LocalDateTime validUntil;
    private String message;
}
