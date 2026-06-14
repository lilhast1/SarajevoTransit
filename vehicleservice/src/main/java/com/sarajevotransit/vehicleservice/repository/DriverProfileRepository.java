package com.sarajevotransit.vehicleservice.repository;

import com.sarajevotransit.vehicleservice.model.DriverProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DriverProfileRepository extends JpaRepository<DriverProfile, Long> {
    Optional<DriverProfile> findByUserId(Long userId);
    boolean existsByUserId(Long userId);
}
