package com.sarajevotransit.vehicleservice.messaging;

import com.sarajevotransit.vehicleservice.config.VehicleRabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class VehicleEventPublisher {

    private final RabbitTemplate vehicleRabbitTemplate;

    public void publishMaintenanceOverdue(Long vehicleId, String registrationNumber, String lineCode, long daysOverdue) {
        Map<String, Object> event = Map.of(
                "vehicleId", vehicleId,
                "registrationNumber", registrationNumber,
                "lineCode", lineCode != null ? lineCode : "",
                "daysOverdue", daysOverdue,
                "type", "MAINTENANCE_OVERDUE"
        );
        log.info("Publishing maintenance overdue event for vehicle {}", registrationNumber);
        vehicleRabbitTemplate.convertAndSend(VehicleRabbitMQConfig.VEHICLE_EXCHANGE,
                VehicleRabbitMQConfig.ROUTING_MAINTENANCE, event);
    }

    public void publishDriverServiceRequest(Long driverId, Long vehicleId, String vehicleReg, String description) {
        Map<String, Object> event = Map.of(
                "driverId", driverId,
                "vehicleId", vehicleId,
                "vehicleRegistrationNumber", vehicleReg,
                "description", description,
                "type", "DRIVER_SERVICE_REQUEST"
        );
        log.info("Publishing driver service request for vehicle {}", vehicleReg);
        vehicleRabbitTemplate.convertAndSend(VehicleRabbitMQConfig.VEHICLE_EXCHANGE,
                VehicleRabbitMQConfig.ROUTING_DRIVER_REQUEST, event);
    }

    public void publishDriverAssignmentConflict(Long driverId, String driverName, Long vehicleId,
                                                 String vehicleReg, String lineCode, String date) {
        Map<String, Object> event = Map.of(
                "driverId", driverId,
                "driverName", driverName != null ? driverName : "",
                "vehicleId", vehicleId,
                "vehicleRegistrationNumber", vehicleReg,
                "lineCode", lineCode != null ? lineCode : "",
                "assignmentDate", date,
                "type", "DRIVER_ASSIGNMENT_CONFLICT"
        );
        log.info("Publishing driver assignment conflict for driver {} on vehicle {}", driverId, vehicleReg);
        vehicleRabbitTemplate.convertAndSend(VehicleRabbitMQConfig.VEHICLE_EXCHANGE,
                VehicleRabbitMQConfig.ROUTING_DRIVER_CONFLICT, event);
    }

    public void publishMaintenanceStatusChanged(Long vehicleId, String registrationNumber,
                                                String newStatus, Long driverId) {
        java.util.HashMap<String, Object> event = new java.util.HashMap<>();
        event.put("vehicleId", vehicleId);
        event.put("registrationNumber", registrationNumber);
        event.put("newStatus", newStatus);
        event.put("driverId", driverId != null ? driverId : "");
        event.put("type", "MAINTENANCE_STATUS_CHANGED");
        log.info("Publishing maintenance status change for vehicle {} → {}", registrationNumber, newStatus);
        vehicleRabbitTemplate.convertAndSend(VehicleRabbitMQConfig.VEHICLE_EXCHANGE,
                VehicleRabbitMQConfig.ROUTING_MAINTENANCE_STATUS, event);
    }
}
