package com.sarajevotransit.vehicleservice.repository;

import com.sarajevotransit.vehicleservice.model.DriverAvailability;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DriverAvailabilityRepository extends JpaRepository<DriverAvailability, Long> {
    List<DriverAvailability> findByDriverIdAndAvailableDateBetween(Long driverId, LocalDate from, LocalDate to);
    Optional<DriverAvailability> findByDriverIdAndAvailableDate(Long driverId, LocalDate date);
    List<DriverAvailability> findByDriverIdAndAvailableDateGreaterThanEqualAndAvailableTrue(Long driverId, LocalDate from);
}
