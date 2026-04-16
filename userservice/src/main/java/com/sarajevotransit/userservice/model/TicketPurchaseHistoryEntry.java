package com.sarajevotransit.userservice.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ticket_purchase_history_entries")
@Getter
@Setter
public class TicketPurchaseHistoryEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull
    private UserProfile user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @NotNull
    private TicketType ticketType;

    @Column(nullable = false, precision = 10, scale = 2)
    @NotNull
    @DecimalMin(value = "0.1")
    private BigDecimal amount;

    @Column(nullable = false)
    @NotBlank
    @Size(max = 60)
    private String paymentMethod;

    @Column(nullable = false)
    @NotBlank
    @Size(max = 120)
    private String externalTransactionId;

    @Column(name = "line_code", length = 40)
    @Size(max = 40)
    private String lineCode;

    @Column(nullable = false)
    @NotNull
    private LocalDateTime purchasedAt;

    @PrePersist
    public void prePersist() {
        if (this.purchasedAt == null) {
            this.purchasedAt = LocalDateTime.now();
        }
    }
}
