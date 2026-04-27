package com.sarajevotransit.vehicleservice.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sarajevotransit.vehicleservice.model.VehicleLocationHistory;

public interface VehicleLocationRepository extends JpaRepository<VehicleLocationHistory, Long> {

    List<VehicleLocationHistory> findByVehicleIdAndTimestampBetweenOrderByTimestampAsc(
            Long vehicleId, LocalDateTime start, LocalDateTime end);

    // Latest known position for a vehicle (complements the denormalized
    // lastLat/lastLon on Vehicle)
    Optional<VehicleLocationHistory> findFirstByVehicleIdOrderByTimestampDesc(Long vehicleId);

    // Useful for cleanup jobs — delete history older than a cutoff
    void deleteByVehicleIdAndTimestampBefore(Long vehicleId, LocalDateTime cutoff);

    // How many location pings in a time window — good for "was GPS active?" checks
    long countByVehicleIdAndTimestampBetween(
            Long vehicleId, LocalDateTime start, LocalDateTime end);
}