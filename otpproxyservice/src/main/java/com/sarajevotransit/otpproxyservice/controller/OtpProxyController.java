package com.sarajevotransit.otpproxyservice.controller;

import com.sarajevotransit.otpproxyservice.dto.StopsCountResponse;
import com.sarajevotransit.otpproxyservice.service.OtpProxyService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
