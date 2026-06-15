package com.sarajevotransit.feedbackservice.controller;

import com.sarajevotransit.feedbackservice.dto.AdminFeedbackStatsResponse;
import com.sarajevotransit.feedbackservice.service.AdminFeedbackStatsService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/reports/admin")
@RequiredArgsConstructor
public class AdminFeedbackStatsController {

    private final AdminFeedbackStatsService statsService;

    @GetMapping("/stats")
    public AdminFeedbackStatsResponse getStats(HttpServletRequest request) {
        if (!"ADMIN".equals(request.getHeader("X-User-Role"))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return statsService.getStats();
    }
}
