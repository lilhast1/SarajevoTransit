package com.sarajevotransit.otpproxyservice.controller;

import com.sarajevotransit.otpproxyservice.dto.OptimalRouteResponse;
import com.sarajevotransit.otpproxyservice.dto.StopsCountResponse;
import com.sarajevotransit.otpproxyservice.service.OtpProxyService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalTime;

@Validated
@RestController
@RequestMapping("/api/v1/proxy")
public class OtpProxyController {

    private final OtpProxyService otpProxyService;

    public OtpProxyController(OtpProxyService otpProxyService) {
        this.otpProxyService = otpProxyService;
    }

    @GetMapping("/stops-count")
    public StopsCountResponse stopsCount() {
        return otpProxyService.fetchStopsCount();
    }

    @GetMapping("/optimal-route")
    public OptimalRouteResponse optimalRoute(
            @RequestParam @Min(-90) @Max(90) double fromLat,
            @RequestParam @Min(-180) @Max(180) double fromLon,
            @RequestParam @Min(-90) @Max(90) double toLat,
            @RequestParam @Min(-180) @Max(180) double toLon,
            @RequestParam(required = false) String modes,
            @RequestParam(required = false) Boolean arriveBy,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) @DateTimeFormat(pattern = "HH:mm") LocalTime time,
            @RequestParam(required = false) @Min(0) Integer maxWalkDistance,
            @RequestParam(required = false) @Min(0) Integer maxTransfers,
            @RequestParam(required = false) Boolean wheelchair,
            @RequestParam(required = false) @Min(1) Integer numItineraries
    ) {
        return otpProxyService.fetchOptimalRoute(
                fromLat,
                fromLon,
                toLat,
                toLon,
                modes,
                arriveBy,
                date,
                time,
                maxWalkDistance,
                maxTransfers,
                wheelchair,
                numItineraries
        );
    }
}
