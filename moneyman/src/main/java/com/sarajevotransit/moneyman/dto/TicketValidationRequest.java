package com.sarajevotransit.moneyman.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TicketValidationRequest {
    @NotBlank(message = "QR code data is required")
    private String qrCodeData;

    public String getQrCodeData() {
        return qrCodeData;
    }
}
