package com.sarajevotransit.vehicleservice.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sarajevotransit.vehicleservice.dtos.*;
import com.sarajevotransit.vehicleservice.service.MaintenanceAlertService;
import com.sarajevotransit.vehicleservice.mappers.LocationHistoryMapper;
import com.sarajevotransit.vehicleservice.mappers.ServiceRecordMapper;
import com.sarajevotransit.vehicleservice.mappers.VehicleMapper;
import com.sarajevotransit.vehicleservice.model.ServiceRecord;
import com.sarajevotransit.vehicleservice.model.Vehicle;
import com.sarajevotransit.vehicleservice.service.VehicleService;
import com.sarajevotransit.vehicleservice.service.MaintenanceAlertService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.web.bind.annotation.RequestBody;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@RestController
@RequestMapping("/api/vehicles")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class VehicleController {

    private final VehicleService vehicleService;
    private final MaintenanceAlertService maintenanceAlertService;
    private final VehicleMapper vehicleMapper;
    private final ServiceRecordMapper serviceRecordMapper;
    private final LocationHistoryMapper locationHistoryMapper;

    // ── Vehicles ────────────────────────────────────────────────

    @GetMapping
    @Operation(security = {})
    public ResponseEntity<Page<VehicleResponseDTO>> getAllVehicles(
            @PageableDefault(size = 50, sort = "manufactureDate", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<Vehicle> vehicles = vehicleService.getAllVehicles(pageable);
        Page<VehicleResponseDTO> response = vehicles.map(vehicleMapper::toResponse);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(security = {})
    public ResponseEntity<VehicleResponseDTO> getVehicleById(@PathVariable Long id) {
        return ResponseEntity.ok(vehicleMapper.toResponse(vehicleService.getVehicleById(id)));
    }

    @PostMapping
    public ResponseEntity<VehicleResponseDTO> addVehicle(@RequestBody @Valid CreateVehicleRequestDto dto) {
        Vehicle vehicle = vehicleMapper.toEntity(dto);
        Long id = vehicleService.addVehicle(vehicle);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(vehicleMapper.toResponse(vehicleService.getVehicleById(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<VehicleResponseDTO> updateVehicle(
            @PathVariable Long id,
            @RequestBody UpdateVehicleFullDto dto) {
        Vehicle updated = vehicleService.updateVehicleFull(id,
                dto.getRegistrationNumber(), dto.getInternalId(), dto.getType(),
                dto.getCapacity(), dto.getManufactureDate(), dto.getStatus(),
                dto.getServiceCycleIntervalDays());
        return ResponseEntity.ok(vehicleMapper.toResponse(updated));
    }

    @PatchMapping("/{id}/assign-line")
    public ResponseEntity<VehicleResponseDTO> assignLine(
            @PathVariable Long id,
            @RequestBody AssignLineRequestDto dto) {
        Vehicle updated = vehicleService.assignLine(id, dto.getLineId(), dto.getLineCode(), dto.getLineName());
        return ResponseEntity.ok(vehicleMapper.toResponse(updated));
    }

    @PatchMapping("/{id}/assign-driver")
    public ResponseEntity<VehicleResponseDTO> assignDriver(
            @PathVariable Long id,
            @RequestBody java.util.Map<String, Long> body) {
        Long driverId = body.get("driverId");
        Vehicle updated = vehicleService.assignDriver(id, driverId);
        return ResponseEntity.ok(vehicleMapper.toResponse(updated));
    }

    @GetMapping("/maintenance-alerts")
    @Operation(security = {})
    public ResponseEntity<java.util.List<MaintenanceAlertDto>> getMaintenanceAlerts() {
        return ResponseEntity.ok(maintenanceAlertService.getOverdueVehicles());
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<VehicleResponseDTO> setVehicleStatus(
            @PathVariable Long id,
            @RequestBody @Valid VehicleStatusUpdateDto dto) {
        return ResponseEntity.ok(
                vehicleMapper.toResponse(vehicleService.setVehicleStatus(id, dto.getStatus())));
    }

    @PatchMapping("/status/batch")
    public ResponseEntity<List<VehicleResponseDTO>> batchUpdateVehicleStatus(
            @RequestBody @Valid VehicleBatchStatusUpdateDto dto) {
        List<Vehicle> updatedVehicles = vehicleService.batchUpdateStatus(dto.getUpdates());
        List<VehicleResponseDTO> response = updatedVehicles.stream()
                .map(vehicleMapper::toResponse)
                .toList();
        return ResponseEntity.ok(response);
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<java.util.Map<String, String>> handleIllegalState(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(java.util.Map.of("message", ex.getMessage()));
    }

    // ── Location ─────────────────────────────────────────────────

    @PostMapping("/{id}/location")
    public ResponseEntity<VehicleResponseDTO> updateLocation(
            @PathVariable Long id,
            @RequestBody @Valid LocationUpdateRequestDto dto) {
        return ResponseEntity.ok(
                vehicleMapper.toResponse(vehicleService.updatePosition(id, dto.getLongitude(), dto.getLatitude())));
    }

    @GetMapping("/{id}/location/history")
    public ResponseEntity<List<LocationHistoryResponseDto>> getLocationHistory(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return ResponseEntity.ok(
                locationHistoryMapper.toResponseList(vehicleService.getGPSroute(id, from, to)));
    }

    // ── Service records ──────────────────────────────────────────

    @GetMapping("/{id}/service-records")
    public ResponseEntity<List<ServiceRecordResponseDto>> getServiceHistory(@PathVariable Long id) {
        return ResponseEntity.ok(
                serviceRecordMapper.toResponseList(vehicleService.getServiceHistory(id)));
    }

    @GetMapping("/{id}/service-records/interval")
    public ResponseEntity<List<ServiceRecordResponseDto>> getServiceHistoryInInterval(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return ResponseEntity.ok(
                serviceRecordMapper.toResponseList(vehicleService.getServiceHistoryInInterval(id, from, to)));
    }

    @PostMapping("/{id}/service-records")
    public ResponseEntity<ServiceRecordResponseDto> addServiceRecord(
            @PathVariable Long id,
            @RequestBody @Valid CreateServiceRecordRequestDto dto) {
        ServiceRecord saved = vehicleService.addServiceRecordForVehicle(
                id,
                dto.getServiceStart(),
                dto.getServiceEnd(),
                dto.getDescription(),
                dto.getPartsChanged(),
                dto.getCost());
        return ResponseEntity.status(HttpStatus.CREATED).body(serviceRecordMapper.toResponse(saved));
    }
}
