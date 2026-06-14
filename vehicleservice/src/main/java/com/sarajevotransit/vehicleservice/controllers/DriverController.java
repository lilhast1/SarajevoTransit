package com.sarajevotransit.vehicleservice.controllers;

import com.sarajevotransit.vehicleservice.dtos.*;
import com.sarajevotransit.vehicleservice.model.*;
import com.sarajevotransit.vehicleservice.model.enums.RequestStatus;
import com.sarajevotransit.vehicleservice.model.enums.VehicleType;
import com.sarajevotransit.vehicleservice.service.DriverService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/vehicles/drivers")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class DriverController {

    private final DriverService driverService;

    // ── Profiles ──────────────────────────────────────────────────────────────

    @GetMapping
    public ResponseEntity<List<DriverProfileDto>> getAllDrivers() {
        return ResponseEntity.ok(driverService.getAllDrivers().stream()
                .map(this::toDto).collect(Collectors.toList()));
    }

    @GetMapping("/{driverId}")
    public ResponseEntity<DriverProfileDto> getDriver(@PathVariable Long driverId) {
        return ResponseEntity.ok(toDto(driverService.getDriverById(driverId)));
    }

    @GetMapping("/by-user/{userId}")
    public ResponseEntity<DriverProfileDto> getDriverByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(toDto(driverService.getDriverByUserId(userId)));
    }

    @PostMapping
    public ResponseEntity<DriverProfileDto> createDriver(
            @RequestBody @Valid CreateDriverProfileDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(toDto(driverService.createDriver(dto)));
    }

    @PutMapping("/{driverId}")
    public ResponseEntity<DriverProfileDto> updateDriver(
            @PathVariable Long driverId,
            @RequestBody CreateDriverProfileDto dto) {
        return ResponseEntity.ok(toDto(driverService.updateDriver(driverId, dto)));
    }

    // ── Availability ──────────────────────────────────────────────────────────

    @GetMapping("/{driverId}/availability")
    public ResponseEntity<List<DriverAvailabilityDto>> getAvailability(
            @PathVariable Long driverId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        if (from == null) from = LocalDate.now().withDayOfMonth(1);
        if (to == null) to = from.plusMonths(2);
        return ResponseEntity.ok(driverService.getAvailability(driverId, from, to).stream()
                .map(this::toAvailDto).collect(Collectors.toList()));
    }

    @PostMapping("/{driverId}/availability")
    public ResponseEntity<DriverAvailabilityDto> setAvailability(
            @PathVariable Long driverId,
            @RequestBody @Valid SetAvailabilityRequestDto dto) {
        return ResponseEntity.ok(
                toAvailDto(driverService.setAvailability(driverId, dto.getDate(), dto.isAvailable())));
    }

    // ── Assignments ───────────────────────────────────────────────────────────

    @GetMapping("/{driverId}/assignments")
    public ResponseEntity<List<DriverAssignmentDto>> getAssignments(@PathVariable Long driverId) {
        return ResponseEntity.ok(driverService.getAssignmentsByDriver(driverId).stream()
                .map(this::toAssignDto).collect(Collectors.toList()));
    }

    // ── Statistics ────────────────────────────────────────────────────────────

    @GetMapping("/{driverId}/statistics")
    public ResponseEntity<DriverStatisticsDto> getStatistics(@PathVariable Long driverId) {
        return ResponseEntity.ok(driverService.getStatistics(driverId));
    }

    // ── Available drivers for a date ──────────────────────────────────────────

    @GetMapping("/available")
    public ResponseEntity<List<DriverProfileDto>> getAvailableDrivers(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) VehicleType vehicleType) {
        return ResponseEntity.ok(driverService.getAvailableDriversForDate(date, vehicleType).stream()
                .map(this::toDto).collect(Collectors.toList()));
    }

    // ── Driver service requests ───────────────────────────────────────────────

    @PostMapping("/{driverId}/service-requests")
    public ResponseEntity<DriverServiceRequestDto> createServiceRequest(
            @PathVariable Long driverId,
            @RequestBody @Valid CreateDriverServiceRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(toSvcReqDto(driverService.createServiceRequest(driverId, dto)));
    }

    @GetMapping("/{driverId}/service-requests")
    public ResponseEntity<List<DriverServiceRequestDto>> getDriverServiceRequests(
            @PathVariable Long driverId) {
        return ResponseEntity.ok(driverService.getServiceRequestsByDriver(driverId).stream()
                .map(this::toSvcReqDto).collect(Collectors.toList()));
    }

    // ── Mappers ───────────────────────────────────────────────────────────────

    private DriverProfileDto toDto(DriverProfile p) {
        DriverProfileDto dto = new DriverProfileDto();
        dto.setId(p.getId());
        dto.setUserId(p.getUserId());
        dto.setFullName(p.getFullName());
        dto.setPhone(p.getPhone());
        dto.setLicenseNumber(p.getLicenseNumber());
        dto.setLicensedVehicleTypes(p.getLicensedVehicleTypes());
        dto.setCreatedAt(p.getCreatedAt());
        return dto;
    }

    private DriverAvailabilityDto toAvailDto(DriverAvailability a) {
        DriverAvailabilityDto dto = new DriverAvailabilityDto();
        dto.setId(a.getId());
        dto.setDriverId(a.getDriverId());
        dto.setAvailableDate(a.getAvailableDate());
        dto.setAvailable(a.isAvailable());
        return dto;
    }

    private DriverAssignmentDto toAssignDto(DriverAssignment a) {
        DriverAssignmentDto dto = new DriverAssignmentDto();
        dto.setId(a.getId());
        dto.setDriverId(a.getDriverId());
        dto.setVehicleId(a.getVehicleId());
        dto.setLineId(a.getLineId());
        dto.setLineCode(a.getLineCode());
        dto.setLineName(a.getLineName());
        dto.setAssignmentDate(a.getAssignmentDate());
        dto.setActive(a.isActive());
        dto.setCreatedAt(a.getCreatedAt());
        return dto;
    }

    private DriverServiceRequestDto toSvcReqDto(DriverServiceRequest r) {
        DriverServiceRequestDto dto = new DriverServiceRequestDto();
        dto.setId(r.getId());
        dto.setDriverId(r.getDriverId());
        dto.setVehicleId(r.getVehicleId());
        dto.setDescription(r.getDescription());
        dto.setStatus(r.getStatus());
        dto.setResolutionNote(r.getResolutionNote());
        dto.setResolvedByUserId(r.getResolvedByUserId());
        dto.setRequestedAt(r.getRequestedAt());
        dto.setResolvedAt(r.getResolvedAt());
        return dto;
    }
}
