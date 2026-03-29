package com.sarajevotransit.moneyman.model;

import com.sarajevotransit.moneyman.model.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
@Getter @Setter @NoArgsConstructor
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    @Column(precision = 10, scale = 2)
    private BigDecimal amount;

    private String currency; // "BAM"

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    private String externalTransactionId;
    private LocalDateTime createdAt;

    // Optional: bidirectional link so you can see the ticket from the transaction
    @OneToOne(mappedBy = "transaction", cascade = CascadeType.ALL)
    private Ticket ticket;
}
