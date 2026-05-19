package com.sarajevotransit.vehicleservice.controllers;

import com.sarajevotransit.vehicleservice.dtos.CreateVehicleStatusRequestDto;
import com.sarajevotransit.vehicleservice.dtos.ResolveVehicleStatusRequestDto;
import com.sarajevotransit.vehicleservice.dtos.VehicleStatusRequestResponseDto;
import com.sarajevotransit.vehicleservice.mappers.VehicleStatusRequestMapper;
import com.sarajevotransit.vehicleservice.model.enums.RequestStatus;
import com.sarajevotransit.vehicleservice.service.VehicleStatusRequestService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vehicles")
@RequiredArgsConstructor
public class VehicleStatusRequestController {

    private final VehicleStatusRequestService requestService;
    private final VehicleStatusRequestMapper requestMapper;

    @PostMapping("/{vehicleId}/status/requests")
    public ResponseEntity<VehicleStatusRequestResponseDto> createRequest(
            @PathVariable Long vehicleId,
            @RequestBody @Valid CreateVehicleStatusRequestDto dto) {
        var saved = requestService.createRequest(
                vehicleId, dto.getRequestedStatus(), dto.getRequestedByUserId(), dto.getNotes());
        return ResponseEntity.status(HttpStatus.CREATED).body(requestMapper.toResponse(saved));
    }

    @GetMapping("/{vehicleId}/status/requests")
    @Operation(security = {})
    public ResponseEntity<List<VehicleStatusRequestResponseDto>> getRequestsForVehicle(
            @PathVariable Long vehicleId,
            @RequestParam(required = false) RequestStatus status) {
        var list = (status == RequestStatus.PENDING)
                ? requestService.getPendingForVehicle(vehicleId)
                : requestService.getRequestsForVehicle(vehicleId);
        return ResponseEntity.ok(requestMapper.toResponseList(list));
    }

    @GetMapping("/status/requests")
    @Operation(security = {})
    public ResponseEntity<List<VehicleStatusRequestResponseDto>> getAllRequests(
            @RequestParam(required = false) Long requestedByUserId) {
        var list = (requestedByUserId != null)
                ? requestService.getByUser(requestedByUserId)
                : requestService.getAll();
        return ResponseEntity.ok(requestMapper.toResponseList(list));
    }

    @PatchMapping("/status/requests/{requestId}/resolve")
    public ResponseEntity<VehicleStatusRequestResponseDto> resolveRequest(
            @PathVariable Long requestId,
            @RequestBody @Valid ResolveVehicleStatusRequestDto dto) {
        var resolved = requestService.resolve(
                requestId, dto.getResolution(), dto.getResolvedByUserId(), dto.getResolutionNote());
        return ResponseEntity.ok(requestMapper.toResponse(resolved));
    }
}
