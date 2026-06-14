package com.sarajevotransit.vehicleservice.controllers;

import com.sarajevotransit.vehicleservice.dtos.*;
import com.sarajevotransit.vehicleservice.model.DriverAssignment;
import com.sarajevotransit.vehicleservice.model.DriverServiceRequest;
import com.sarajevotransit.vehicleservice.model.enums.RequestStatus;
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
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/vehicles/driver-assignments")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class DriverAssignmentController {

    private final DriverService driverService;

    @GetMapping
    public ResponseEntity<List<DriverAssignmentDto>> getAssignments(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        if (date == null) date = LocalDate.now();
        return ResponseEntity.ok(driverService.getAssignmentsByDate(date).stream()
                .map(this::toDto).collect(Collectors.toList()));
    }

    @PostMapping
    public ResponseEntity<DriverAssignmentDto> createAssignment(
            @RequestBody @Valid CreateDriverAssignmentDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(toDto(driverService.createAssignment(dto)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelAssignment(@PathVariable Long id) {
        driverService.cancelAssignment(id);
        return ResponseEntity.noContent().build();
    }

    // ── Driver service requests (admin endpoint) ───────────────────────────────

    @GetMapping("/service-requests")
    public ResponseEntity<List<DriverServiceRequestDto>> getAllServiceRequests(
            @RequestParam(required = false) RequestStatus status) {
        List<DriverServiceRequest> requests = status != null
                ? driverService.getServiceRequestsByStatus(status)
                : driverService.getAllServiceRequests();
        return ResponseEntity.ok(requests.stream().map(this::toSvcDto).collect(Collectors.toList()));
    }

    @PatchMapping("/service-requests/{requestId}")
    public ResponseEntity<DriverServiceRequestDto> resolveServiceRequest(
            @PathVariable Long requestId,
            @RequestBody @Valid ResolveDriverServiceRequestDto dto,
            HttpServletRequest request) {
        String userIdHeader = request.getHeader("X-User-Id");
        Long userId = userIdHeader != null ? Long.parseLong(userIdHeader) : null;
        return ResponseEntity.ok(toSvcDto(driverService.resolveServiceRequest(requestId, dto, userId)));
    }

    private DriverAssignmentDto toDto(DriverAssignment a) {
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

    private DriverServiceRequestDto toSvcDto(DriverServiceRequest r) {
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
