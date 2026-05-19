package com.sarajevotransit.feedbackservice.client;

import com.sarajevotransit.feedbackservice.exception.BadRequestException;
import com.sarajevotransit.feedbackservice.exception.ServiceUnavailableException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class NotificationServiceClient {

    private final RestClient.Builder restClientBuilder;
    private final LoadBalancerClient loadBalancerClient;

    @Value("${service.notification.id:notificationservice}")
    private String notificationServiceId;

    public void notifyReportStatusChange(Long reportId, Long lineId, Long userId, String newStatus) {
        postNotification(Map.of(
                "userId", userId,
                "lineId", lineId,
                "type", "GENERAL",
                "title", "Problem report status changed",
                "content", "Report " + reportId + " is now " + newStatus));
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
