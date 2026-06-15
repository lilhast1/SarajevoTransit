package ba.unsa.etf.pnwt.routingservice.controller;

import ba.unsa.etf.pnwt.routingservice.event.TimetableChangedEvent;
import ba.unsa.etf.pnwt.routingservice.messaging.TimetableEventPublisher;
import ba.unsa.etf.pnwt.routingservice.model.OtpRebuildState;
import ba.unsa.etf.pnwt.routingservice.service.OtpRebuildService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
@Tag(name = "OTP Rebuild", description = "OTP graph rebuild state management")
public class OtpRebuildController {

    private final OtpRebuildService otpRebuildService;
    private final TimetableEventPublisher timetableEventPublisher;

    @Value("${routing.gtfs.admin-token:}")
    private String adminToken;

    public OtpRebuildController(OtpRebuildService otpRebuildService, TimetableEventPublisher timetableEventPublisher) {
        this.otpRebuildService = otpRebuildService;
        this.timetableEventPublisher = timetableEventPublisher;
    }

    @GetMapping("/otp-rebuild-state")
    @Operation(summary = "Get current OTP rebuild state")
    public ResponseEntity<Map<String, Object>> getRebuildState() {
        OtpRebuildState state = otpRebuildService.get();
        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("needsRebuild", state.isNeedsRebuild());
        body.put("rebuildInProgress", state.isRebuildInProgress());
        body.put("lastDataChangeAt", state.getLastDataChangeAt() != null ? state.getLastDataChangeAt().toString() : null);
        body.put("lastRebuildAt", state.getLastRebuildAt() != null ? state.getLastRebuildAt().toString() : null);
        body.put("rebuildTriggeredBy", state.getRebuildTriggeredBy() != null ? state.getRebuildTriggeredBy() : null);
        body.put("status", state.getStatus().name());
        body.put("errorMessage", state.getErrorMessage() != null ? state.getErrorMessage() : null);
        return ResponseEntity.ok(body);
    }

    @PostMapping("/otp-rebuild/trigger")
    @Operation(summary = "Trigger OTP graph rebuild (admin only)")
    public ResponseEntity<Map<String, String>> triggerRebuild(
            @RequestHeader(value = "X-User-Role", required = false) String userRole,
            @RequestHeader(value = "X-User-Id", required = false) String userId
    ) {
        if (!"ADMIN".equals(userRole)) {
            throw new ba.unsa.etf.pnwt.routingservice.exception.ForbiddenException("Admin access required");
        }

        OtpRebuildState state = otpRebuildService.get();
        if (state.isRebuildInProgress()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Rebuild already in progress",
                    "status", state.getStatus().name()
            ));
        }
        if (!state.isNeedsRebuild()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "No rebuild needed — data has not changed since last rebuild"
            ));
        }

        timetableEventPublisher.publishTimetableChanged(new TimetableChangedEvent(
                UUID.randomUUID().toString(),
                null,
                null,
                null,
                "REBUILD_TRIGGERED",
                Instant.now()
        ));

        otpRebuildService.markRebuildStarted(userId != null ? userId : "admin");

        return ResponseEntity.accepted().body(Map.of(
                "message", "Rebuild triggered successfully",
                "status", "BUILDING_GRAPH"
        ));
    }

    @PostMapping("/otp-rebuild/complete")
    @Operation(summary = "Mark rebuild as completed (internal — called by otpproxyservice)")
    public ResponseEntity<Map<String, String>> completeRebuild(
            @RequestHeader(value = "X-Internal-Token", required = false) String internalToken
    ) {
        authorizeInternal(internalToken);
        otpRebuildService.markRebuildCompleted();
        return ResponseEntity.ok(Map.of("message", "Rebuild marked as completed"));
    }

    @PostMapping("/otp-rebuild/fail")
    @Operation(summary = "Mark rebuild as failed (internal — called by otpproxyservice)")
    public ResponseEntity<Map<String, String>> failRebuild(
            @RequestHeader(value = "X-Internal-Token", required = false) String internalToken,
            @RequestHeader(value = "X-Error-Message", required = false) String errorMessage
    ) {
        authorizeInternal(internalToken);
        otpRebuildService.markRebuildFailed(errorMessage != null ? errorMessage : "Unknown error");
        return ResponseEntity.ok(Map.of("message", "Rebuild marked as failed"));
    }

    @PostMapping("/otp-rebuild/status")
    @Operation(summary = "Update rebuild status (internal — called by otpproxyservice)")
    public ResponseEntity<Map<String, String>> updateStatus(
            @RequestHeader(value = "X-Internal-Token", required = false) String internalToken,
            @RequestHeader(value = "X-Rebuild-Status", required = false) String status
    ) {
        authorizeInternal(internalToken);
        try {
            OtpRebuildState.RebuildStatus rebuildStatus = OtpRebuildState.RebuildStatus.valueOf(status);
            otpRebuildService.updateStatus(rebuildStatus);
            return ResponseEntity.ok(Map.of("message", "Status updated to " + status));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid status: " + status));
        }
    }

    private void authorizeInternal(String internalToken) {
        if (adminToken == null || adminToken.isBlank()) {
            return;
        }
        if (!adminToken.equals(internalToken)) {
            throw new ba.unsa.etf.pnwt.routingservice.exception.ForbiddenException("Invalid internal token");
        }
    }
}
