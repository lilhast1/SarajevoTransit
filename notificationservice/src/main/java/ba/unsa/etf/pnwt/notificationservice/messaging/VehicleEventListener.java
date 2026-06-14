package ba.unsa.etf.pnwt.notificationservice.messaging;

import ba.unsa.etf.pnwt.notificationservice.config.RabbitMQConfig;
import ba.unsa.etf.pnwt.notificationservice.model.Notification;
import ba.unsa.etf.pnwt.notificationservice.model.NotificationType;
import ba.unsa.etf.pnwt.notificationservice.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class VehicleEventListener {

    private final NotificationRepository notificationRepository;
    private final RestTemplate restTemplate;

    @RabbitListener(queues = RabbitMQConfig.QUEUE_VEHICLE_MAINTENANCE)
    public void onMaintenanceOverdue(Map<String, Object> event) {
        String reg = String.valueOf(event.getOrDefault("registrationNumber", "unknown"));
        Object days = event.get("daysOverdue");
        String title = "Vehicle Maintenance Overdue: " + reg;
        String content = "Vehicle " + reg + " is " + days + " day(s) overdue for scheduled service.";
        notifyAdmins(title, content, NotificationType.VEHICLE_MAINTENANCE_ALERT);
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_DRIVER_SERVICE_REQUEST)
    public void onDriverServiceRequest(Map<String, Object> event) {
        String reg = String.valueOf(event.getOrDefault("vehicleRegistrationNumber", "unknown"));
        String desc = String.valueOf(event.getOrDefault("description", ""));
        String title = "Driver Service Request for Vehicle " + reg;
        String content = "A driver has requested extra service for vehicle " + reg + ". Reason: " + desc;
        notifyAdmins(title, content, NotificationType.DRIVER_SERVICE_REQUEST);
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_DRIVER_CONFLICT)
    public void onDriverConflict(Map<String, Object> event) {
        String driverName = String.valueOf(event.getOrDefault("driverName", "Driver"));
        String lineCode = String.valueOf(event.getOrDefault("lineCode", ""));
        String date = String.valueOf(event.getOrDefault("assignmentDate", ""));
        String reg = String.valueOf(event.getOrDefault("vehicleRegistrationNumber", ""));
        String title = "Driver Assignment Conflict on " + date;
        String content = driverName + " is assigned to vehicle " + reg + " on line " + lineCode
                + " for " + date + " but has become unavailable.";
        notifyAdmins(title, content, NotificationType.DRIVER_ASSIGNMENT_CONFLICT);
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_MAINTENANCE_STATUS)
    public void onMaintenanceStatusChanged(Map<String, Object> event) {
        String reg = String.valueOf(event.getOrDefault("registrationNumber", "unknown"));
        String newStatus = String.valueOf(event.getOrDefault("newStatus", ""));
        String title = "Vehicle Status Changed: " + reg;
        String content = "Vehicle " + reg + " has automatically changed status to " + newStatus
                + " based on scheduled service record.";
        notifyAdmins(title, content, NotificationType.VEHICLE_MAINTENANCE_ALERT);
    }

    private void notifyAdmins(String title, String content, NotificationType type) {
        List<Long> adminIds = fetchAdminUserIds();
        if (adminIds.isEmpty()) {
            log.warn("No admin users found for notification: {}", title);
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        for (Long adminId : adminIds) {
            Notification n = new Notification();
            n.setUserId(adminId);
            n.setType(type);
            n.setTitle(title);
            n.setContent(content);
            n.setIsRead(false);
            n.setSentAt(now);
            notificationRepository.save(n);
        }
        log.info("Sent '{}' notification to {} admin(s)", title, adminIds.size());
    }

    @SuppressWarnings("unchecked")
    private List<Long> fetchAdminUserIds() {
        try {
            var response = restTemplate.exchange(
                    "http://user-service/api/v1/users/by-role/ADMIN",
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            );
            if (response.getBody() == null) return Collections.emptyList();
            return response.getBody().stream()
                    .map(m -> ((Number) m.get("id")).longValue())
                    .toList();
        } catch (Exception e) {
            log.error("Failed to fetch admin users from user-service: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}
