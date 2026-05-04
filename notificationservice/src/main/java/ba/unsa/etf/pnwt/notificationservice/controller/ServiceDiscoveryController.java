package ba.unsa.etf.pnwt.notificationservice.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;

@RestController
@RequestMapping({ "/api/discovery", "/api/v1/discovery" })
public class ServiceDiscoveryController {

    @Value("${spring.application.name:notifications}")
    private String serviceName;

    @Value("${eureka.instance.instance-id:${spring.application.name:notifications}:${server.port:8086}}")
    private String instanceId;

    @GetMapping("/instance")
    public ServiceInstanceInfoResponse getCurrentInstance(HttpServletRequest request) {
        return new ServiceInstanceInfoResponse(
                serviceName,
                instanceId,
                request.getServerName(),
                request.getLocalPort(),
                OffsetDateTime.now().toString());
    }

    public record ServiceInstanceInfoResponse(
            String serviceName,
            String instanceId,
            String host,
            int port,
            String timestamp) {}
}
