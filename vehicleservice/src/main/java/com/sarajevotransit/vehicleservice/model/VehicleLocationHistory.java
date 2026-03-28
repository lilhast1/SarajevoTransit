package com.sarajevotransit.vehicleservice.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "vehicle_location_history", indexes = {
        @Index(name = "idx_vehicle_time", columnList = "vehicle_id, timestamp")
})
@Getter
@Setter
public class VehicleLocationHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long vehicleId; // Use simple ID here to avoid heavy Join overhead
    private Double latitude;
    private Double longitude;
    private Double speed;
    private LocalDateTime timestamp;
}