package com.sarajevotransit.vehicleservice.controllers;

import com.sarajevotransit.vehicleservice.dtos.CreateVehicleRequestDto;
import com.sarajevotransit.vehicleservice.dtos.UpdateVehicleRequestDto;
import com.sarajevotransit.vehicleservice.dtos.VehicleResponseDTO;
import com.sarajevotransit.vehicleservice.dtos.VehicleStatusUpdateDto;
import com.sarajevotransit.vehicleservice.model.Vehicle;
import com.sarajevotransit.vehicleservice.model.enums.VehicleStatus;
import com.sarajevotransit.vehicleservice.model.enums.VehicleType;
import com.sarajevotransit.vehicleservice.service.VehicleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VehicleControllerTest {

    @Mock
    private VehicleService vehicleService;

    @Mock
    private com.sarajevotransit.vehicleservice.mappers.VehicleMapper vehicleMapper;

    @Mock
    private com.sarajevotransit.vehicleservice.mappers.ServiceRecordMapper serviceRecordMapper;

    @Mock
    private com.sarajevotransit.vehicleservice.mappers.LocationHistoryMapper locationHistoryMapper;

    @InjectMocks
    private VehicleController vehicleController;

    private Vehicle testVehicle;
    private VehicleResponseDTO testVehicleResponse;

    @BeforeEach
    void setUp() {
        testVehicle = createTestVehicle(1L, "ABC-123", VehicleType.BUS);
        testVehicleResponse = createTestVehicleResponse(1L, "ABC-123", VehicleType.BUS);
    }

    @Test
    void getAllVehicles_ShouldReturnListOfVehicles() {
        // Given
        List<Vehicle> vehicles = Arrays.asList(testVehicle, createTestVehicle(2L, "DEF-456", VehicleType.TRAM));
        Page<Vehicle> vehiclePage = new PageImpl<>(vehicles);
        List<VehicleResponseDTO> expectedResponses = Arrays.asList(
                testVehicleResponse,
                createTestVehicleResponse(2L, "DEF-456", VehicleType.TRAM));

        when(vehicleService.getAllVehicles(any())).thenReturn(vehiclePage);
        when(vehicleMapper.toResponse(testVehicle)).thenReturn(testVehicleResponse);
        Pageable pageable = PageRequest.of(0, 10);
        when(vehicleMapper.toResponse(vehicles.get(1))).thenReturn(expectedResponses.get(1));

        // When
        ResponseEntity<Page<VehicleResponseDTO>> response = vehicleController.getAllVehicles(null);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().getContent().size());
        assertEquals("ABC-123", response.getBody().getContent().get(0).getRegistrationNumber());
        assertEquals("DEF-456", response.getBody().getContent().get(1).getRegistrationNumber());
    }

    @Test
    void getVehicleById_ShouldReturnVehicle() {
        // Given
        when(vehicleService.getVehicleById(1L)).thenReturn(testVehicle);
        when(vehicleMapper.toResponse(testVehicle)).thenReturn(testVehicleResponse);

        // When
        ResponseEntity<VehicleResponseDTO> response = vehicleController.getVehicleById(1L);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1L, response.getBody().getId());
        assertEquals("ABC-123", response.getBody().getRegistrationNumber());
        assertEquals(VehicleType.BUS, response.getBody().getType());
        assertEquals(VehicleStatus.OPERATIONAL, response.getBody().getStatus());
    }

    @Test
    void addVehicle_ShouldCreateAndReturnId() {
        // Given
        CreateVehicleRequestDto requestDto = new CreateVehicleRequestDto(
                "ABC-123", "INT-001", VehicleType.BUS, 50,
                LocalDate.of(2020, 1, 1), VehicleStatus.OPERATIONAL,
                43.8563, 18.4131, LocalDateTime.now());

        when(vehicleMapper.toEntity(requestDto)).thenReturn(testVehicle);
        when(vehicleService.addVehicle(testVehicle)).thenReturn(1L);

        // When
        ResponseEntity<Long> response = vehicleController.addVehicle(requestDto);

        // Then
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(1L, response.getBody());
    }


    @Test
    void setVehicleStatus_ShouldUpdateStatus() {
        // Given
        VehicleStatusUpdateDto statusDto = new VehicleStatusUpdateDto(VehicleStatus.OUT_OF_SERVICE);
        Vehicle updatedVehicle = createTestVehicle(1L, "ABC-123", VehicleType.BUS);
        updatedVehicle.setStatus(VehicleStatus.OUT_OF_SERVICE);
        VehicleResponseDTO updatedResponse = new VehicleResponseDTO(1L, "ABC-123", "INT-1", VehicleType.BUS, 50,
                LocalDate.of(2020, 1, 1), VehicleStatus.OUT_OF_SERVICE, 43.8563, 18.4131,
                LocalDateTime.now(), 1L);

        when(vehicleService.setVehicleStatus(1L, VehicleStatus.OUT_OF_SERVICE)).thenReturn(updatedVehicle);
        when(vehicleMapper.toResponse(updatedVehicle)).thenReturn(updatedResponse);

        // When
        ResponseEntity<VehicleResponseDTO> response = vehicleController.setVehicleStatus(1L, statusDto);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1L, response.getBody().getId());
        assertEquals(VehicleStatus.OUT_OF_SERVICE, response.getBody().getStatus());
    }

    private Vehicle createTestVehicle(Long id, String registrationNumber, VehicleType type) {
        Vehicle vehicle = new Vehicle();
        vehicle.setId(id);
        vehicle.setRegistrationNumber(registrationNumber);
        vehicle.setInternalId("INT-" + id);
        vehicle.setType(type);
        vehicle.setCapacity(50);
        vehicle.setManufactureDate(LocalDate.of(2020, 1, 1));
        vehicle.setStatus(VehicleStatus.OPERATIONAL);
        vehicle.setLastLat(43.8563);
        vehicle.setLastLon(18.4131);
        vehicle.setLastGpsUpdate(LocalDateTime.now());
        vehicle.setVersion(1L);
        return vehicle;
    }

    private VehicleResponseDTO createTestVehicleResponse(Long id, String registrationNumber, VehicleType type) {
        return new VehicleResponseDTO(id, registrationNumber, "INT-" + id, type, 50,
                LocalDate.of(2020, 1, 1), VehicleStatus.OPERATIONAL, 43.8563, 18.4131,
                LocalDateTime.now(), 1L);
    }
}