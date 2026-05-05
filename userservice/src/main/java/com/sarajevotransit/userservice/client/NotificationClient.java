package com.sarajevotransit.userservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.sarajevotransit.userservice.dto.notification.CreateNotificationRequest;

@FeignClient(name = "notificationservice")
public interface NotificationClient {

    @PostMapping("/notifications")
    void createNotification(@RequestBody CreateNotificationRequest request);
}