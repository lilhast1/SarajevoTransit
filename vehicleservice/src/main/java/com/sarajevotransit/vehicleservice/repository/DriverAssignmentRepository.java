package com.sarajevotransit.vehicleservice.repository;

import com.sarajevotransit.vehicleservice.model.DriverAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DriverAssignmentRepository extends JpaRepository<DriverAssignment, Long> {
    List<DriverAssignment> findByAssignmentDateAndActiveTrue(LocalDate date);
    List<DriverAssignment> findByDriverIdOrderByAssignmentDateDesc(Long driverId);
    List<DriverAssignment> findByVehicleIdOrderByAssignmentDateDesc(Long vehicleId);
    Optional<DriverAssignment> findByDriverIdAndAssignmentDateAndActiveTrue(Long driverId, LocalDate date);
    boolean existsByDriverIdAndAssignmentDateAndActiveTrue(Long driverId, LocalDate date);

    @Query("SELECT a FROM DriverAssignment a WHERE a.assignmentDate = :date AND a.active = true AND a.driverId IN :driverIds")
    List<DriverAssignment> findByAssignmentDateAndDriverIdIn(LocalDate date, List<Long> driverIds);
}
