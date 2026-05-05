package com.sarajevotransit.moneyman.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "payment_methods")
@Getter @Setter @NoArgsConstructor
public class PaymentMethod {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId; // Link to User Service

    private String provider; // e.g., "STRIPE", "PAYPAL"
    private String gatewayToken; // The token returned by the provider

    private String lastFour; // To show the user: "Visa ending in 4242"
    private String cardType; // "VISA", "MASTERCARD"

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getProvider() {
        return provider;
    }

    public String getGatewayToken() {
        return gatewayToken;
    }

    public String getLastFour() {
        return lastFour;
    }

    public String getCardType() {
        return cardType;
    }

    public boolean isDefault() {
        return isDefault;
    }

    private boolean isDefault;
}