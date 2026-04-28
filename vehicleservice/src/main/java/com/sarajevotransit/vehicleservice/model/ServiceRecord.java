package com.sarajevotransit.vehicleservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "service_records")
@Getter
@Setter
@NoArgsConstructor
public class ServiceRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id")
    private Vehicle vehicle;

    private LocalDateTime serviceStart;
    private LocalDateTime serviceEnd;

    private String description; // e.g., "Engine overhaul"
    private BigDecimal cost;
    private String partsChanged;

    // Logic for "How long was it out of service" can be a method:
    public long getDowntimeInHours() {
        if (serviceStart != null && serviceEnd != null) {
            return java.time.Duration.between(serviceStart, serviceEnd).toHours();
        }
        return 0;
    }
}
