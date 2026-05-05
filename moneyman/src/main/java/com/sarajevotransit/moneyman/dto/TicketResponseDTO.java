package com.sarajevotransit.moneyman.dto;

import com.sarajevotransit.moneyman.model.enums.TicketStatus;
import com.sarajevotransit.moneyman.model.enums.TicketType;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class TicketResponseDTO {
    private UUID id;
    private TicketType type;
    private TicketStatus status;

    public UUID getId() {
        return id;
    }

    public TicketType getType() {
        return type;
    }

    public TicketStatus getStatus() {
        return status;
    }

    public LocalDateTime getPurchaseDate() {
        return purchaseDate;
    }

    public LocalDateTime getValidUntil() {
        return validUntil;
    }

    public String getQrCodeData() {
        return qrCodeData;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getExternalTransactionId() {
        return externalTransactionId;
    }

    private LocalDateTime purchaseDate;
    private LocalDateTime validUntil;
    private String qrCodeData;

    // This will be extracted from the Transaction entity
    private BigDecimal amount;
    private String externalTransactionId;
}