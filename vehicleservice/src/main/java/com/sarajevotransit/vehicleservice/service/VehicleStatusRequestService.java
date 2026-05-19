package com.sarajevotransit.vehicleservice.service;

import com.sarajevotransit.vehicleservice.model.VehicleStatusRequest;
import com.sarajevotransit.vehicleservice.model.enums.RequestStatus;
import com.sarajevotransit.vehicleservice.model.enums.VehicleStatus;
import com.sarajevotransit.vehicleservice.repository.VehicleStatusRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VehicleStatusRequestService {

    private final VehicleStatusRequestRepository repository;
    private final VehicleService vehicleService;

    public VehicleStatusRequest createRequest(Long vehicleId, VehicleStatus requestedStatus,
                                              Long requestedByUserId, String notes) {
        var vehicle = vehicleService.getVehicleById(vehicleId);
        var request = new VehicleStatusRequest();
        request.setVehicle(vehicle);
        request.setRequestedStatus(requestedStatus);
        request.setRequestedByUserId(requestedByUserId);
        request.setNotes(notes);
        request.setRequestStatus(RequestStatus.PENDING);
        request.setRequestedAt(LocalDateTime.now());
        return repository.save(request);
    }

    public List<VehicleStatusRequest> getRequestsForVehicle(Long vehicleId) {
        return repository.findByVehicle_IdOrderByRequestedAtDesc(vehicleId);
    }

    public List<VehicleStatusRequest> getPendingForVehicle(Long vehicleId) {
        return repository.findByVehicle_IdAndRequestStatusOrderByRequestedAtDesc(
                vehicleId, RequestStatus.PENDING);
    }

    public List<VehicleStatusRequest> getByUser(Long userId) {
        return repository.findByRequestedByUserIdOrderByRequestedAtDesc(userId);
    }

    public List<VehicleStatusRequest> getAll() {
        return repository.findAllByOrderByRequestedAtDesc();
    }

    public VehicleStatusRequest resolve(Long requestId, RequestStatus resolution,
                                        Long resolvedByUserId, String resolutionNote) {
        if (resolution == RequestStatus.PENDING) {
            throw new IllegalArgumentException("Resolution must be APPROVED or REJECTED");
        }
        var request = repository.findById(requestId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException(
                        "Status request not found: " + requestId));

        if (request.getRequestStatus() != RequestStatus.PENDING) {
            throw new IllegalStateException("Request " + requestId + " is already resolved");
        }

        request.setRequestStatus(resolution);
        request.setResolvedByUserId(resolvedByUserId);
        request.setResolutionNote(resolutionNote);
        request.setResolvedAt(LocalDateTime.now());

        if (resolution == RequestStatus.APPROVED) {
            vehicleService.setVehicleStatus(request.getVehicle().getId(), request.getRequestedStatus());
        }

        return repository.save(request);
    }
}
