package com.sarajevotransit.vehicleservice.service;

import com.sarajevotransit.vehicleservice.dtos.*;
import com.sarajevotransit.vehicleservice.messaging.VehicleEventPublisher;
import com.sarajevotransit.vehicleservice.model.*;
import com.sarajevotransit.vehicleservice.model.enums.RequestStatus;
import com.sarajevotransit.vehicleservice.model.enums.VehicleType;
import com.sarajevotransit.vehicleservice.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DriverService {

    private final DriverProfileRepository driverProfileRepository;
    private final DriverAvailabilityRepository availabilityRepository;
    private final DriverAssignmentRepository assignmentRepository;
    private final DriverServiceRequestRepository serviceRequestRepository;
    private final VehicleRepository vehicleRepository;
    private final VehicleEventPublisher eventPublisher;

    // ── Profiles ──────────────────────────────────────────────────────────────

    public List<DriverProfile> getAllDrivers() {
        return driverProfileRepository.findAll();
    }

    public DriverProfile getDriverById(Long driverId) {
        return driverProfileRepository.getReferenceById(driverId);
    }

    public DriverProfile getDriverByUserId(Long userId) {
        return driverProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Driver profile not found for userId: " + userId));
    }

    @Transactional
    public DriverProfile createDriver(CreateDriverProfileDto dto) {
        if (driverProfileRepository.existsByUserId(dto.getUserId())) {
            throw new IllegalStateException("Driver profile already exists for userId: " + dto.getUserId());
        }
        DriverProfile profile = new DriverProfile();
        profile.setUserId(dto.getUserId());
        profile.setFullName(dto.getFullName());
        profile.setPhone(dto.getPhone());
        profile.setLicenseNumber(dto.getLicenseNumber());
        if (dto.getLicensedVehicleTypes() != null) {
            profile.setLicensedVehicleTypes(dto.getLicensedVehicleTypes());
        }
        return driverProfileRepository.save(profile);
    }

    @Transactional
    public DriverProfile updateDriver(Long driverId, CreateDriverProfileDto dto) {
        DriverProfile profile = driverProfileRepository.getReferenceById(driverId);
        if (dto.getFullName() != null) profile.setFullName(dto.getFullName());
        if (dto.getPhone() != null) profile.setPhone(dto.getPhone());
        if (dto.getLicenseNumber() != null) profile.setLicenseNumber(dto.getLicenseNumber());
        if (dto.getLicensedVehicleTypes() != null) profile.setLicensedVehicleTypes(dto.getLicensedVehicleTypes());
        return driverProfileRepository.save(profile);
    }

    // ── Availability ──────────────────────────────────────────────────────────

    public List<DriverAvailability> getAvailability(Long driverId, LocalDate from, LocalDate to) {
        return availabilityRepository.findByDriverIdAndAvailableDateBetween(driverId, from, to);
    }

    @Transactional
    public DriverAvailability setAvailability(Long driverId, LocalDate date, boolean available) {
        DriverAvailability entry = availabilityRepository
                .findByDriverIdAndAvailableDate(driverId, date)
                .orElseGet(() -> {
                    DriverAvailability a = new DriverAvailability();
                    a.setDriverId(driverId);
                    a.setAvailableDate(date);
                    return a;
                });
        boolean wasAvailable = entry.isAvailable();
        entry.setAvailable(available);
        DriverAvailability saved = availabilityRepository.save(entry);

        // If driver just became unavailable, check for active assignments and emit conflict events
        if (wasAvailable && !available) {
            checkAndPublishConflict(driverId, date);
        }
        return saved;
    }

    private void checkAndPublishConflict(Long driverId, LocalDate date) {
        assignmentRepository.findByDriverIdAndAssignmentDateAndActiveTrue(driverId, date)
                .ifPresent(assignment -> {
                    DriverProfile profile = driverProfileRepository.getReferenceById(driverId);
                    Vehicle vehicle = vehicleRepository.getReferenceById(assignment.getVehicleId());
                    eventPublisher.publishDriverAssignmentConflict(
                            driverId,
                            profile.getFullName(),
                            assignment.getVehicleId(),
                            vehicle.getRegistrationNumber(),
                            assignment.getLineCode(),
                            date.toString()
                    );
                    log.warn("Driver {} marked unavailable but has active assignment on {}", driverId, date);
                });
    }

    // ── Assignments ───────────────────────────────────────────────────────────

    public List<DriverAssignment> getAssignmentsByDate(LocalDate date) {
        return assignmentRepository.findByAssignmentDateAndActiveTrue(date);
    }

    public List<DriverAssignment> getAssignmentsByDriver(Long driverId) {
        return assignmentRepository.findByDriverIdOrderByAssignmentDateDesc(driverId);
    }

    public List<DriverAssignment> getAssignmentsByVehicle(Long vehicleId) {
        return assignmentRepository.findByVehicleIdOrderByAssignmentDateDesc(vehicleId);
    }

    @Transactional
    public DriverAssignment createAssignment(CreateDriverAssignmentDto dto) {
        if (assignmentRepository.existsByDriverIdAndAssignmentDateAndActiveTrue(dto.getDriverId(), dto.getAssignmentDate())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Driver already has an active assignment on " + dto.getAssignmentDate());
        }
        DriverAssignment assignment = new DriverAssignment();
        assignment.setDriverId(dto.getDriverId());
        assignment.setVehicleId(dto.getVehicleId());
        assignment.setLineId(dto.getLineId());
        assignment.setLineCode(dto.getLineCode());
        assignment.setLineName(dto.getLineName());
        assignment.setAssignmentDate(dto.getAssignmentDate());
        return assignmentRepository.save(assignment);
    }

    @Transactional
    public void cancelAssignment(Long assignmentId) {
        DriverAssignment assignment = assignmentRepository.getReferenceById(assignmentId);
        assignment.setActive(false);
        assignmentRepository.save(assignment);
    }

    // ── Service requests ──────────────────────────────────────────────────────

    public List<DriverServiceRequest> getAllServiceRequests() {
        return serviceRequestRepository.findAllByOrderByRequestedAtDesc();
    }

    public List<DriverServiceRequest> getServiceRequestsByStatus(RequestStatus status) {
        return serviceRequestRepository.findByStatusOrderByRequestedAtDesc(status);
    }

    public List<DriverServiceRequest> getServiceRequestsByDriver(Long driverId) {
        return serviceRequestRepository.findByDriverIdOrderByRequestedAtDesc(driverId);
    }

    @Transactional
    public DriverServiceRequest createServiceRequest(Long driverId, CreateDriverServiceRequestDto dto) {
        DriverServiceRequest req = new DriverServiceRequest();
        req.setDriverId(driverId);
        req.setVehicleId(dto.getVehicleId());
        req.setDescription(dto.getDescription());
        DriverServiceRequest saved = serviceRequestRepository.save(req);

        // Notify admin via RabbitMQ
        Vehicle vehicle = vehicleRepository.getReferenceById(dto.getVehicleId());
        eventPublisher.publishDriverServiceRequest(driverId, dto.getVehicleId(),
                vehicle.getRegistrationNumber(), dto.getDescription());
        return saved;
    }

    @Transactional
    public DriverServiceRequest resolveServiceRequest(Long requestId, ResolveDriverServiceRequestDto dto, Long resolvedByUserId) {
        DriverServiceRequest req = serviceRequestRepository.getReferenceById(requestId);
        req.setStatus(dto.getStatus());
        req.setResolutionNote(dto.getResolutionNote());
        req.setResolvedByUserId(resolvedByUserId);
        req.setResolvedAt(LocalDateTime.now());
        return serviceRequestRepository.save(req);
    }

    // ── Statistics ────────────────────────────────────────────────────────────

    public DriverStatisticsDto getStatistics(Long driverId) {
        List<DriverAssignment> assignments = assignmentRepository.findByDriverIdOrderByAssignmentDateDesc(driverId);
        long total = assignments.size();
        long daysActive = assignments.stream().filter(DriverAssignment::isActive).count();
        List<String> lines = assignments.stream()
                .filter(a -> a.getLineCode() != null)
                .map(DriverAssignment::getLineCode)
                .distinct()
                .collect(Collectors.toList());
        List<Long> vehicles = assignments.stream()
                .map(DriverAssignment::getVehicleId)
                .distinct()
                .collect(Collectors.toList());
        return new DriverStatisticsDto(total, daysActive, lines, vehicles);
    }

    // ── Helper: drivers available for a vehicle type on a date ────────────────

    public List<DriverProfile> getAvailableDriversForDate(LocalDate date, VehicleType vehicleType) {
        return driverProfileRepository.findAll().stream()
                .filter(d -> vehicleType == null || d.getLicensedVehicleTypes().contains(vehicleType))
                .filter(d -> {
                    var avail = availabilityRepository.findByDriverIdAndAvailableDate(d.getId(), date);
                    return avail.map(DriverAvailability::isAvailable).orElse(false);
                })
                .filter(d -> !assignmentRepository.existsByDriverIdAndAssignmentDateAndActiveTrue(d.getId(), date))
                .collect(Collectors.toList());
    }
}
