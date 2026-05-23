package ba.unsa.etf.pnwt.routingservice.controller;

import ba.unsa.etf.pnwt.routingservice.service.TripRealtimeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/realtime/gtfsrt")
@Tag(name = "GTFS Realtime", description = "Public GTFS-RT realtime feed endpoints")
public class GtfsRealtimeController {

    private final TripRealtimeService tripRealtimeService;

    public GtfsRealtimeController(TripRealtimeService tripRealtimeService) {
        this.tripRealtimeService = tripRealtimeService;
    }

    @GetMapping(value = "/trip-updates.pb", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @Operation(summary = "Get GTFS-RT TripUpdates feed")
    public ResponseEntity<byte[]> tripUpdates() {
        return ResponseEntity.ok(tripRealtimeService.buildTripUpdatesFeed());
    }
}
