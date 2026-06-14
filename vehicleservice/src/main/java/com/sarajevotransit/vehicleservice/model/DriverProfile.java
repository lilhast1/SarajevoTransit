package com.sarajevotransit.vehicleservice.model;

import com.sarajevotransit.vehicleservice.model.enums.VehicleType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "driver_profiles")
@Getter
@Setter
@NoArgsConstructor
public class DriverProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private Long userId;

    private String fullName;
    private String phone;
    private String licenseNumber;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "driver_licensed_types", joinColumns = @JoinColumn(name = "driver_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "vehicle_type")
    private List<VehicleType> licensedVehicleTypes = new ArrayList<>();

    private LocalDateTime createdAt = LocalDateTime.now();
}
