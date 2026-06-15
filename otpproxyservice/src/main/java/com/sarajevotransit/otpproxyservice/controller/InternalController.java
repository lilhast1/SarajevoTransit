package com.sarajevotransit.otpproxyservice.controller;

import com.sarajevotransit.otpproxyservice.service.OtpColorState;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/internal")
public class InternalController {

    private final OtpColorState colorState;

    @Value("${routing.admin-token:}")
    private String adminToken;

    public InternalController(OtpColorState colorState) {
        this.colorState = colorState;
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status(
            @RequestHeader(value = "X-Internal-Token", required = false) String token
    ) {
        authorize(token);
        return ResponseEntity.ok(Map.of(
                "activeColor", colorState.getActiveColor(),
                "inactiveColor", colorState.getInactiveColor(),
                "activeUrl", colorState.getActiveUrl(),
                "inactiveUrl", colorState.getInactiveUrl(),
                "rebuilding", colorState.isRebuilding()
        ));
    }

    @PostMapping("/switch-color")
    public ResponseEntity<Map<String, String>> switchColor(
            @RequestHeader(value = "X-Internal-Token", required = false) String token,
            @RequestBody Map<String, String> body
    ) {
        authorize(token);
        String color = body.get("color");
        if (color == null || (!color.equalsIgnoreCase("BLUE") && !color.equalsIgnoreCase("GREEN"))) {
            return ResponseEntity.badRequest().body(Map.of("error", "Color must be BLUE or GREEN"));
        }
        colorState.switchTo(color);
        return ResponseEntity.ok(Map.of("message", "Traffic switched to " + color.toUpperCase()));
    }

    private void authorize(String token) {
        if (adminToken == null || adminToken.isBlank()) {
            return;
        }
        if (!adminToken.equals(token)) {
            throw new SecurityException("Invalid internal token");
        }
    }
}
