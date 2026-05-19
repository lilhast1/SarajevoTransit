package ba.unsa.etf.pnwt.routingservice.controller;

import ba.unsa.etf.pnwt.routingservice.dto.TripDelayResponse;
import ba.unsa.etf.pnwt.routingservice.dto.TripDelayUpsertRequest;
import ba.unsa.etf.pnwt.routingservice.exception.ForbiddenException;
import ba.unsa.etf.pnwt.routingservice.service.TripRealtimeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/realtime")
@Tag(name = "Realtime Admin", description = "Admin endpoints for trip-level realtime delays")
@SecurityRequirement(name = "bearerAuth")
public class TripRealtimeAdminController {

    private final TripRealtimeService tripRealtimeService;

    @Value("${routing.realtime.admin-token:}")
    private String adminToken;

    public TripRealtimeAdminController(TripRealtimeService tripRealtimeService) {
        this.tripRealtimeService = tripRealtimeService;
    }

    @PostMapping("/trip-delays")
    @Operation(summary = "Set or update trip delay")
    public ResponseEntity<TripDelayResponse> setDelay(
            @RequestHeader(value = "X-Admin-Token", required = false) String requestToken,
            @Valid @RequestBody TripDelayUpsertRequest request
    ) {
        authorize(requestToken);
        return ResponseEntity.status(HttpStatus.CREATED).body(tripRealtimeService.setDelay(request));
    }

    @DeleteMapping("/trip-delays/{timetableId}")
    @Operation(summary = "Clear trip delay by timetable and service date")
    public ResponseEntity<Void> clearDelay(
            @RequestHeader(value = "X-Admin-Token", required = false) String requestToken,
            @PathVariable Integer timetableId,
            @RequestParam String serviceDate
    ) {
        authorize(requestToken);
        tripRealtimeService.clearDelay(timetableId, serviceDate);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/trip-delays")
    @Operation(summary = "List in-memory active trip delays")
    public ResponseEntity<List<TripDelayResponse>> getDelays(
            @RequestHeader(value = "X-Admin-Token", required = false) String requestToken
    ) {
        authorize(requestToken);
        return ResponseEntity.ok(tripRealtimeService.getDelays());
    }

    private void authorize(String requestToken) {
        if (adminToken == null || adminToken.isBlank()) {
            throw new IllegalStateException("Realtime admin token is not configured");
        }
        if (!adminToken.equals(requestToken)) {
            throw new ForbiddenException("Invalid admin token");
        }
    }
}
