package com.sarajevotransit.feedbackservice.client;

import com.sarajevotransit.feedbackservice.exception.ServiceUnavailableException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class NotificationServiceClient {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceClient.class);

    private final RestClient.Builder restClientBuilder;
    private final LoadBalancerClient loadBalancerClient;

    @Value("${service.notification.id:notificationservice}")
    private String notificationServiceId;

    public void notifyReportStatusChange(Long reportId, Long lineId, Long userId, String category, String newStatus) {
        String humanStatus = switch (newStatus) {
            case "IN_PROGRESS" -> "In Progress";
            case "RESOLVED"    -> "Resolved";
            default            -> "Received";
        };
        String humanCategory = category == null ? "problem" :
                category.charAt(0) + category.substring(1).toLowerCase().replace('_', ' ');

        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", userId);
        payload.put("type", "REPORT_STATUS_CHANGE");
        payload.put("title", "Report status updated to " + humanStatus);
        payload.put("content", "Your " + humanCategory + " report #" + reportId + " is now " + humanStatus + ".");
        if (lineId != null) payload.put("lineId", lineId);

        postNotification(payload);
    }

    public void notifyModerationFlag(Long reviewId, Long lineId, Long userId) {
        postNotification(Map.of(
                "userId", userId,
                "lineId", lineId,
                "type", "GENERAL",
                "title", "Review flagged for moderation",
                "content", "Review " + reviewId + " was flagged for moderation on line " + lineId));
    }

    private void postNotification(Map<String, Object> payload) {
        ServiceInstance instance = loadBalancerClient.choose(notificationServiceId);
        if (instance == null) {
            throw new ServiceUnavailableException("Notification service is unavailable.");
        }

        String url = instance.getUri() + "/notifications";
        try {
            restClientBuilder.build()
                    .post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-User-Role", "ADMIN")
                    .header("X-User-Id", "0")
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException exception) {
            throw new ServiceUnavailableException("Notification service is unavailable.");
        }
    }
}
