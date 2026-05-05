package com.sarajevotransit.vehicleservice.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sarajevotransit.vehicleservice.model.ServiceRecord;

public interface ServiceRecordRepository extends JpaRepository<ServiceRecord, Long> {
    // Basic find by vehicle id (traverses the relationship)
    List<ServiceRecord> findByVehicleId(Long vehicleId);

    // With sorting — most recent first
    List<ServiceRecord> findByVehicleIdOrderByServiceStartDesc(Long vehicleId);

    // Useful for "is this vehicle currently in service?"
    Optional<ServiceRecord> findFirstByVehicleIdAndServiceEndIsNullOrderByServiceStartDesc(Long vehicleId);

    // Date range — e.g. service history for last 6 months
    List<ServiceRecord> findByVehicleIdAndServiceStartBetween(
            Long vehicleId, LocalDateTime from, LocalDateTime to);

    // Check if any open (unfinished) record exists — good for validation before
    // status change
    boolean existsByVehicleIdAndServiceEndIsNull(Long vehicleId);
}