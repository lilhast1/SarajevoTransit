package com.sarajevotransit.userservice.controller;

import com.sarajevotransit.userservice.dto.DiscoveryPingResponse;
import com.sarajevotransit.userservice.service.FeedbackDiscoveryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping({ "/api/discovery", "/api/v1/discovery" })
public class DiscoveryBenchmarkController {

    private final FeedbackDiscoveryService feedbackDiscoveryService;

    @GetMapping("/feedback/ping")
    public DiscoveryPingResponse pingFeedback(
            @RequestParam(defaultValue = "lb") String mode) {
        return feedbackDiscoveryService.ping(mode);
    }
}
