package com.sarajevotransit.vehicleservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "driver_availability",
        uniqueConstraints = @UniqueConstraint(columnNames = {"driver_id", "available_date"}))
@Getter
@Setter
@NoArgsConstructor
public class DriverAvailability {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "driver_id", nullable = false)
    private Long driverId;

    @Column(name = "available_date", nullable = false)
    private LocalDate availableDate;

    private boolean available = true;
}
