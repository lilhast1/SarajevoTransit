package com.sarajevotransit.vehicleservice.service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sarajevotransit.vehicleservice.model.ServiceRecord;
import com.sarajevotransit.vehicleservice.model.Vehicle;
import com.sarajevotransit.vehicleservice.model.VehicleLocationHistory;
import com.sarajevotransit.vehicleservice.model.enums.VehicleStatus;
import com.sarajevotransit.vehicleservice.repository.ServiceRecordRepository;
import com.sarajevotransit.vehicleservice.repository.VehicleLocationRepository;
import com.sarajevotransit.vehicleservice.repository.VehicleRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class VehicleService {
    private static final Logger logger = LoggerFactory.getLogger(VehicleService.class);
    private final VehicleRepository vRepository;
    private final VehicleLocationRepository locRepository;
    private final ServiceRecordRepository sRepository;

    public static final Double EARTH_RADIUS = 6371000.0;

    public VehicleService(VehicleRepository vehicleRepository, VehicleLocationRepository vehicleLocationRepository,
            ServiceRecordRepository serviceRecordRepository) {
        this.vRepository = vehicleRepository;
        this.locRepository = vehicleLocationRepository;
        this.sRepository = serviceRecordRepository;
    }

    public List<Vehicle> getAllVehicles() {
        return vRepository.findAll();
    }

    public Vehicle getVehicleById(Long id) {
        return vRepository.getReferenceById(id);
    }

    public Vehicle setVehicleStatus(Long id, VehicleStatus status) {
        var v = this.vRepository.getReferenceById(id);
        v.setStatus(status);
        vRepository.save(v);
        return v;
    }

    public List<ServiceRecord> getServiceHistory(Long vehicleId) {
        return this.sRepository.findByVehicleId(vehicleId);
    }

    public List<ServiceRecord> getServiceHistoryInInterval(Long vehicleId, LocalDateTime from, LocalDateTime to) {
        return this.sRepository.findByVehicleIdAndServiceStartBetween(vehicleId, from, to);
    }

    public ServiceRecord addServiceRecordForVehicle(Long vehicleId, LocalDateTime serviceStart,
            LocalDateTime serviceEnd, String description, String parts, BigDecimal cost) {

        Vehicle vehicle = this.vRepository.getReferenceById(vehicleId);
        ServiceRecord record = new ServiceRecord();
        record.setVehicle(vehicle);
        record.setServiceStart(serviceStart);
        record.setServiceEnd(serviceEnd);
        record.setDescription(description);
        record.setCost(cost);
        record.setPartsChanged(parts);

        return this.sRepository.save(record);
    }

    public Vehicle updatePosition(Long vehicleId, double longitude, double latitude) {
        Vehicle vehicle = this.vRepository.getReferenceById(vehicleId);
        var prevY = vehicle.getLastLat() * Math.PI / 180;
        var prevX = vehicle.getLastLon() * Math.PI / 180;
        var t0 = vehicle.getLastGpsUpdate();
        var t1 = Instant.now();
        var loc = new VehicleLocationHistory();
        loc.setLatitude(prevY);
        loc.setLongitude(prevX);
        loc.setVehicleId(vehicleId);

        var radlat = latitude * Math.PI / 180;
        var radong = longitude * Math.PI / 180;

        var distance = 2 * EARTH_RADIUS * Math.asin(
                Math.sqrt(
                        Math.sin((radlat - prevY) / 2) * Math.sin((radlat - prevY) / 2)
                                + Math.cos(radlat) * Math.cos(prevY) * Math.sin((radong - prevX) / 2)
                                        * Math.sin((radong - prevX) / 2)

                ));

        var speed = distance / Duration.between(t0, t1).getSeconds();
        loc.setSpeed(speed * 3.6);
        loc.setTimestamp(t0);

        this.locRepository.save(loc);

        vehicle.setLastGpsUpdate(LocalDateTime.now());
        vehicle.setLastLat(latitude);
        vehicle.setLastLon(longitude);

        return this.vRepository.save(vehicle);

    }

    public List<VehicleLocationHistory> getGPSroute(Long vehicleId, LocalDateTime from, LocalDateTime to) {
        return this.locRepository.findByVehicleIdAndTimestampBetweenOrderByTimestampAsc(vehicleId, from, to);
    }

    public Long addVehicle(Vehicle v) {
        v = this.vRepository.save(v);
        return v.getId();
    }

    public Vehicle updateVehicle(Vehicle v) {
        return this.vRepository.save(v);
    }

}
