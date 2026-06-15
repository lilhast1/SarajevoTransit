package com.sarajevotransit.vehicleservice.repository;

import com.sarajevotransit.vehicleservice.model.DriverServiceRequest;
import com.sarajevotransit.vehicleservice.model.enums.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DriverServiceRequestRepository extends JpaRepository<DriverServiceRequest, Long> {
    List<DriverServiceRequest> findByDriverIdOrderByRequestedAtDesc(Long driverId);
    List<DriverServiceRequest> findByStatusOrderByRequestedAtDesc(RequestStatus status);
    List<DriverServiceRequest> findAllByOrderByRequestedAtDesc();
}
