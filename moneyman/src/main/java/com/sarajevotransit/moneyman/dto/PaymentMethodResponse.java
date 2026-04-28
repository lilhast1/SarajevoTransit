package com.sarajevotransit.moneyman.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PaymentMethodResponse {
    private Long id;
    private Long userId;
    private String provider;
    private String lastFour;
    private String cardType;
    private boolean isDefault;
}
