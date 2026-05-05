package com.sarajevotransit.vehicleservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.sarajevotransit.vehicleservice.model.enums.VehicleStatus;
import com.sarajevotransit.vehicleservice.model.enums.VehicleType;

@Entity
@Table(name = "vehicles")
@Getter
@Setter
@NoArgsConstructor
public class Vehicle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String registrationNumber; // e.g., "A12-E-345"

    private String internalId; // GRAS internal number, e.g., "401" (for a tram)

    @Enumerated(EnumType.STRING)
    private VehicleType type; // BUS, TRAM, TROLLEY

    private Integer capacity;
    private LocalDate manufactureDate;

    @Enumerated(EnumType.STRING)
    private VehicleStatus status; // OPERATIONAL, IN_MAINTENANCE, OUT_OF_SERVICE, RETIRED

    // We keep the LATEST position here for quick "Where is it now?" reads
    private Double lastLat;
    private Double lastLon;
    private LocalDateTime lastGpsUpdate;

    // Optimistic Locking (standard in JPA to prevent concurrent update issues)
    @Version
    private Long version;

    public Vehicle(
            String registrationNumber,
            String internalId,
            VehicleType type,
            Integer capacity,
            LocalDate manufactureDate,
            VehicleStatus status,
            Double lastLat,
            Double lastLon,
            LocalDateTime lastGpsUpdate) {
        this.registrationNumber = registrationNumber;
        this.internalId = internalId;
        this.type = type;
        this.capacity = capacity;
        this.manufactureDate = manufactureDate;
        this.status = status;
        this.lastLat = lastLat;
        this.lastLon = lastLon;
        this.lastGpsUpdate = lastGpsUpdate;
    }
}