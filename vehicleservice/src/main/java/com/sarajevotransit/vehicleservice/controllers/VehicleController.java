package com.sarajevotransit.vehicleservice.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sarajevotransit.vehicleservice.dtos.CreateServiceRecordRequestDto;
import com.sarajevotransit.vehicleservice.dtos.CreateVehicleRequestDto;
import com.sarajevotransit.vehicleservice.dtos.LocationHistoryResponseDto;
import com.sarajevotransit.vehicleservice.dtos.LocationUpdateRequestDto;
import com.sarajevotransit.vehicleservice.dtos.ServiceRecordResponseDto;
import com.sarajevotransit.vehicleservice.dtos.UpdateVehicleRequestDto;
import com.sarajevotransit.vehicleservice.dtos.VehicleBatchStatusUpdateDto;
import com.sarajevotransit.vehicleservice.dtos.VehicleResponseDTO;
import com.sarajevotransit.vehicleservice.dtos.VehicleStatusBatchItemDto;
import com.sarajevotransit.vehicleservice.dtos.VehicleStatusUpdateDto;
import com.sarajevotransit.vehicleservice.mappers.LocationHistoryMapper;
import com.sarajevotransit.vehicleservice.mappers.ServiceRecordMapper;
import com.sarajevotransit.vehicleservice.mappers.VehicleMapper;
import com.sarajevotransit.vehicleservice.model.ServiceRecord;
import com.sarajevotransit.vehicleservice.model.Vehicle;
import com.sarajevotransit.vehicleservice.service.VehicleService;

import io.swagger.v3.oas.annotations.parameters.RequestBody;
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
public class VehicleController {

    private final VehicleService vehicleService;
    private final VehicleMapper vehicleMapper;
    private final ServiceRecordMapper serviceRecordMapper;
    private final LocationHistoryMapper locationHistoryMapper;

    // ── Vehicles ────────────────────────────────────────────────

    @GetMapping
    public ResponseEntity<Page<VehicleResponseDTO>> getAllVehicles(
            @PageableDefault(size = 50, sort = "manufactureDate", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<Vehicle> vehicles = vehicleService.getAllVehicles(pageable);
        Page<VehicleResponseDTO> response = vehicles.map(vehicleMapper::toResponse);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<VehicleResponseDTO> getVehicleById(@PathVariable Long id) {
        return ResponseEntity.ok(vehicleMapper.toResponse(vehicleService.getVehicleById(id)));
    }

    @PostMapping
    public ResponseEntity<Long> addVehicle(@RequestBody @Valid CreateVehicleRequestDto dto) {
        Vehicle vehicle = vehicleMapper.toEntity(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(vehicleService.addVehicle(vehicle));
    }

    // @PutMapping("/{id}")
    // public ResponseEntity<VehicleResponseDTO> updateVehicle(
    // @PathVariable Long id,
    // @RequestBody @Valid UpdateVehicleRequestDto dto) {
    // Vehicle existing = vehicleService.getVehicleById(id);
    // vehicleMapper.updateEntityFromDto(dto, existing);
    // return
    // ResponseEntity.ok(vehicleMapper.toResponse(vehicleService.updateVehicle(existing)));
    // }

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
