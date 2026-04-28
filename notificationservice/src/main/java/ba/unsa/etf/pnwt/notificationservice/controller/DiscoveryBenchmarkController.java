package ba.unsa.etf.pnwt.notificationservice.controller;

import ba.unsa.etf.pnwt.notificationservice.dto.DiscoveryPingResponse;
import ba.unsa.etf.pnwt.notificationservice.service.NotificationDiscoveryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/discovery", "/api/v1/discovery"})
public class DiscoveryBenchmarkController {

    private final NotificationDiscoveryService notificationDiscoveryService;

    public DiscoveryBenchmarkController(NotificationDiscoveryService notificationDiscoveryService) {
        this.notificationDiscoveryService = notificationDiscoveryService;
    }

    @GetMapping("/ping")
    public DiscoveryPingResponse ping(@RequestParam(defaultValue = "lb") String mode) {
        return notificationDiscoveryService.ping(mode);
    }
}
