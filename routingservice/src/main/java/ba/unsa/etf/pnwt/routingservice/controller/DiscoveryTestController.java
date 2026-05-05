package ba.unsa.etf.pnwt.routingservice.controller;

import ba.unsa.etf.pnwt.routingservice.dto.OtpProxyStopsCountResponse;
import ba.unsa.etf.pnwt.routingservice.service.OtpProxyClientService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/test")
public class DiscoveryTestController {

    private final OtpProxyClientService otpProxyClientService;

    public DiscoveryTestController(OtpProxyClientService otpProxyClientService) {
        this.otpProxyClientService = otpProxyClientService;
    }

    @GetMapping("/otp-stops-count")
    public OtpProxyStopsCountResponse otpStopsCount() {
        return otpProxyClientService.getStopsCount();
    }
}
