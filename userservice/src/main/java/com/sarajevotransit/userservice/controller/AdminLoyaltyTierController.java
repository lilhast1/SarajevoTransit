package com.sarajevotransit.userservice.controller;

import com.sarajevotransit.userservice.dto.LoyaltyTierConfigRequest;
import com.sarajevotransit.userservice.dto.LoyaltyTierConfigResponse;
import com.sarajevotransit.userservice.exception.ForbiddenException;
import com.sarajevotransit.userservice.service.LoyaltyTierConfigService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping({ "/api/admin/loyalty/tiers", "/api/v1/admin/loyalty/tiers" })
@SecurityRequirement(name = "bearerAuth")
public class AdminLoyaltyTierController {

    private final LoyaltyTierConfigService tierConfigService;
    private final HttpServletRequest httpRequest;

    @GetMapping
    public List<LoyaltyTierConfigResponse> getAllTiers() {
        requireAdmin();
        return tierConfigService.getAllTiers();
    }

    @PutMapping("/{id}")
    public ResponseEntity<LoyaltyTierConfigResponse> updateTier(
            @PathVariable Long id,
            @Valid @RequestBody LoyaltyTierConfigRequest request) {
        requireAdmin();
        LoyaltyTierConfigResponse updated = tierConfigService.updateTier(id, request);
        return ResponseEntity.ok(updated);
    }

    private void requireAdmin() {
        String role = httpRequest.getHeader("X-User-Role");
        if (!"ADMIN".equals(role)) {
            throw new ForbiddenException("Admin access required");
        }
    }
}
