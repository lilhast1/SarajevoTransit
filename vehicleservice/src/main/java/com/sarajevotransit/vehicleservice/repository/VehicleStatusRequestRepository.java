package com.sarajevotransit.vehicleservice.repository;

import com.sarajevotransit.vehicleservice.model.VehicleStatusRequest;
import com.sarajevotransit.vehicleservice.model.enums.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VehicleStatusRequestRepository extends JpaRepository<VehicleStatusRequest, Long> {

    List<VehicleStatusRequest> findByVehicle_IdOrderByRequestedAtDesc(Long vehicleId);

    List<VehicleStatusRequest> findByVehicle_IdAndRequestStatusOrderByRequestedAtDesc(
            Long vehicleId, RequestStatus requestStatus);

    List<VehicleStatusRequest> findByRequestStatusOrderByRequestedAtDesc(RequestStatus requestStatus);

    List<VehicleStatusRequest> findByRequestedByUserIdOrderByRequestedAtDesc(Long requestedByUserId);

    List<VehicleStatusRequest> findAllByOrderByRequestedAtDesc();
}
