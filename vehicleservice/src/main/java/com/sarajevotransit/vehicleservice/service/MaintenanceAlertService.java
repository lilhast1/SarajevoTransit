package com.sarajevotransit.vehicleservice.service;

import com.sarajevotransit.vehicleservice.dtos.MaintenanceAlertDto;
import com.sarajevotransit.vehicleservice.messaging.VehicleEventPublisher;
import com.sarajevotransit.vehicleservice.model.Vehicle;
import com.sarajevotransit.vehicleservice.model.ServiceRecord;
import com.sarajevotransit.vehicleservice.model.enums.VehicleStatus;
import com.sarajevotransit.vehicleservice.repository.ServiceRecordRepository;
import com.sarajevotransit.vehicleservice.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MaintenanceAlertService {

    private final VehicleRepository vehicleRepository;
    private final ServiceRecordRepository serviceRecordRepository;
    private final VehicleEventPublisher eventPublisher;
    private final DriverService driverService;

    public List<MaintenanceAlertDto> getOverdueVehicles() {
        List<Vehicle> allVehicles = vehicleRepository.findAll();
        List<MaintenanceAlertDto> alerts = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (Vehicle v : allVehicles) {
            if (v.getServiceCycleIntervalDays() == null) continue;

            // Skip vehicles currently in active service
            if (serviceRecordRepository.existsByVehicleIdAndServiceEndIsNull(v.getId())) continue;

            // Use last completed service record
            List<ServiceRecord> records = serviceRecordRepository.findByVehicleIdOrderByServiceStartDesc(v.getId());
            LocalDateTime lastServiceEnd = records.stream()
                    .filter(r -> r.getServiceEnd() != null)
                    .map(ServiceRecord::getServiceEnd)
                    .max(LocalDateTime::compareTo)
                    .orElse(null);

            if (lastServiceEnd == null) continue;

            long daysSince = ChronoUnit.DAYS.between(lastServiceEnd, now);
            if (daysSince > v.getServiceCycleIntervalDays()) {
                long daysOverdue = daysSince - v.getServiceCycleIntervalDays();
                alerts.add(new MaintenanceAlertDto(
                        v.getId(),
                        v.getRegistrationNumber(),
                        v.getType(),
                        v.getStatus(),
                        v.getAssignedLineCode(),
                        v.getServiceCycleIntervalDays(),
                        lastServiceEnd,
                        daysOverdue
                ));
            }
        }
        return alerts;
    }

    @Scheduled(fixedDelay = 60000)
    public void syncMaintenanceStatuses() {
        LocalDateTime now = LocalDateTime.now(java.time.ZoneOffset.UTC);

        // Vehicles whose service window has started → set IN_MAINTENANCE
        List<ServiceRecord> active = serviceRecordRepository.findCurrentlyActiveServiceRecords(now);
        for (ServiceRecord sr : active) {
            try {
                applyMaintenanceStart(sr);
            } catch (Exception e) {
                log.error("Failed to set IN_MAINTENANCE for service record {}: {}", sr.getId(), e.getMessage());
            }
        }

        // Vehicles whose service window just ended → restore OPERATIONAL
        LocalDateTime windowStart = now.minusSeconds(65);
        List<ServiceRecord> ended = serviceRecordRepository.findRecentlyEndedServiceRecords(windowStart, now);
        for (ServiceRecord sr : ended) {
            try {
                applyMaintenanceEnd(sr);
            } catch (Exception e) {
                log.error("Failed to restore OPERATIONAL for service record {}: {}", sr.getId(), e.getMessage());
            }
        }
    }

    @Transactional
    public void applyMaintenanceStart(ServiceRecord sr) {
        Vehicle v = vehicleRepository.getReferenceById(sr.getVehicle().getId());
        if (v.getStatus() == VehicleStatus.IN_MAINTENANCE) return;

        Long prevDriverId = v.getAssignedDriverId();
        v.setStatus(VehicleStatus.IN_MAINTENANCE);
        v.setAssignedDriverId(null);
        v.setAssignedLineId(null);
        v.setAssignedLineCode(null);
        v.setAssignedLineName(null);
        v.setInternalId(null);
        vehicleRepository.save(v);

        if (prevDriverId != null) {
            try {
                driverService.setAvailability(prevDriverId, sr.getServiceStart().toLocalDate(), false);
            } catch (Exception e) {
                log.warn("Could not update driver {} availability: {}", prevDriverId, e.getMessage());
            }
        }

        eventPublisher.publishMaintenanceStatusChanged(v.getId(), v.getRegistrationNumber(),
                VehicleStatus.IN_MAINTENANCE.name(), prevDriverId);
        log.info("Auto-set vehicle {} to IN_MAINTENANCE (service record {})", v.getRegistrationNumber(), sr.getId());
    }

    @Transactional
    public void applyMaintenanceEnd(ServiceRecord sr) {
        Vehicle v = vehicleRepository.getReferenceById(sr.getVehicle().getId());
        if (v.getStatus() != VehicleStatus.IN_MAINTENANCE) return;

        v.setStatus(VehicleStatus.OPERATIONAL);
        vehicleRepository.save(v);

        eventPublisher.publishMaintenanceStatusChanged(v.getId(), v.getRegistrationNumber(),
                VehicleStatus.OPERATIONAL.name(), null);
        log.info("Auto-restored vehicle {} to OPERATIONAL (service record {})", v.getRegistrationNumber(), sr.getId());
    }

    @Scheduled(cron = "0 0 8 * * *")
    public void checkAndNotifyOverdue() {
        List<MaintenanceAlertDto> overdue = getOverdueVehicles();
        if (overdue.isEmpty()) {
            log.info("Maintenance check: all vehicles up to date");
            return;
        }
        log.warn("Maintenance check: {} vehicles overdue for service", overdue.size());
        for (MaintenanceAlertDto alert : overdue) {
            eventPublisher.publishMaintenanceOverdue(
                    alert.getVehicleId(),
                    alert.getRegistrationNumber(),
                    alert.getAssignedLineCode(),
                    alert.getDaysOverdue()
            );
        }
    }
}
