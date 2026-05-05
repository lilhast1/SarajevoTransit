package com.sarajevotransit.feedbackservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(name = "notificationservice")
public interface NotificationClient {

    @PostMapping("/notifications")
    void createNotification(@RequestBody Map<String, Object> request);
}