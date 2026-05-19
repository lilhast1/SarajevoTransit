package com.sarajevotransit.vehicleservice.model;

import com.sarajevotransit.vehicleservice.model.enums.RequestStatus;
import com.sarajevotransit.vehicleservice.model.enums.VehicleStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "vehicle_status_requests")
@Getter
@Setter
@NoArgsConstructor
public class VehicleStatusRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @Enumerated(EnumType.STRING)
    @Column(name = "requested_status", nullable = false)
    private VehicleStatus requestedStatus;

    @Column(name = "requested_by_user_id", nullable = false)
    private Long requestedByUserId;

    @Column(length = 1000)
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(name = "request_status", nullable = false)
    private RequestStatus requestStatus = RequestStatus.PENDING;

    @Column(name = "resolved_by_user_id")
    private Long resolvedByUserId;

    @Column(name = "resolution_note", length = 500)
    private String resolutionNote;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt = LocalDateTime.now();

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;
}
