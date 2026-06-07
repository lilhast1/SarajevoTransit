package com.sarajevotransit.moneyman.controller;

import com.sarajevotransit.moneyman.dto.AdminStatsResponse;
import com.sarajevotransit.moneyman.service.AdminStatsService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/finance/admin")
@RequiredArgsConstructor
public class AdminStatsController {

    private final AdminStatsService statsService;

    @GetMapping("/stats")
    public AdminStatsResponse getStats(
            @RequestParam(defaultValue = "MONTH") String period,
            HttpServletRequest request) {
        if (!"ADMIN".equals(request.getHeader("X-User-Role"))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return statsService.getStats(period);
    }
}
