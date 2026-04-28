package com.sarajevotransit.moneyman.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PaymentMethodRequest {
    @NotNull(message = "User ID is required")
    private Long userId;

    @NotBlank(message = "Provider is required")
    private String provider;

    @NotBlank(message = "Gateway token is required")
    private String gatewayToken;

    @NotBlank(message = "Last four digits are required")
    @Size(min = 4, max = 4, message = "Last four digits must be exactly 4 characters")
    private String lastFour;

    @NotBlank(message = "Card type is required")
    private String cardType;

    private boolean isDefault;
}
