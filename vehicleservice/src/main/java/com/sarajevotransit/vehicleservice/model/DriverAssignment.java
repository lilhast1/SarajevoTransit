package com.sarajevotransit.vehicleservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "driver_assignments")
@Getter
@Setter
@NoArgsConstructor
public class DriverAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "driver_id", nullable = false)
    private Long driverId;

    @Column(name = "vehicle_id", nullable = false)
    private Long vehicleId;

    private Long lineId;
    private String lineCode;
    private String lineName;

    @Column(name = "assignment_date", nullable = false)
    private LocalDate assignmentDate;

    private boolean active = true;

    private LocalDateTime createdAt = LocalDateTime.now();
}
