package com.sarajevotransit.vehicleservice.model;

import com.sarajevotransit.vehicleservice.model.enums.RequestStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "driver_service_requests")
@Getter
@Setter
@NoArgsConstructor
public class DriverServiceRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "driver_id", nullable = false)
    private Long driverId;

    @Column(name = "vehicle_id", nullable = false)
    private Long vehicleId;

    @Column(nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    private RequestStatus status = RequestStatus.PENDING;

    private String resolutionNote;
    private Long resolvedByUserId;

    private LocalDateTime requestedAt = LocalDateTime.now();
    private LocalDateTime resolvedAt;
}
