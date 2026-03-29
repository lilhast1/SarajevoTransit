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

    private boolean isDefault;
}