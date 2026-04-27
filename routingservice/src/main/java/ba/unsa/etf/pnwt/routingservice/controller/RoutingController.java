package ba.unsa.etf.pnwt.routingservice.controller;

import ba.unsa.etf.pnwt.routingservice.dto.DirectionRequest;
import ba.unsa.etf.pnwt.routingservice.dto.DirectionResponse;
import ba.unsa.etf.pnwt.routingservice.dto.DirectionStationRequest;
import ba.unsa.etf.pnwt.routingservice.dto.DirectionStationResponse;
import ba.unsa.etf.pnwt.routingservice.dto.GeoJsonFeatureResponse;
import ba.unsa.etf.pnwt.routingservice.dto.LineRequest;
import ba.unsa.etf.pnwt.routingservice.dto.LineResponse;
import ba.unsa.etf.pnwt.routingservice.dto.RoutePointRequest;
import ba.unsa.etf.pnwt.routingservice.dto.RoutePointResponse;
import ba.unsa.etf.pnwt.routingservice.dto.StationRequest;
import ba.unsa.etf.pnwt.routingservice.dto.StationResponse;
import ba.unsa.etf.pnwt.routingservice.dto.TimetableRequest;
import ba.unsa.etf.pnwt.routingservice.dto.TimetableResponse;
import ba.unsa.etf.pnwt.routingservice.service.RoutingCrudService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Routing Service", description = "CRUD endpoints for lines, directions, stations, timetables and routing data")
public class RoutingController {

    private final RoutingCrudService routingCrudService;

    public RoutingController(RoutingCrudService routingCrudService) {
        this.routingCrudService = routingCrudService;
    }

    @GetMapping("/lines")
    @Operation(summary = "List lines")
    public ResponseEntity<List<LineResponse>> getLines(
            @RequestParam(required = false) Boolean activeOnly,
            @RequestParam(required = false) Short vehicleTypeId
    ) {
        return ResponseEntity.ok(routingCrudService.getLines(activeOnly, vehicleTypeId));
    }

    @GetMapping("/lines/{id}")
    @Operation(summary = "Get line by id")
    public ResponseEntity<LineResponse> getLine(@PathVariable Integer id) {
        return ResponseEntity.ok(routingCrudService.getLine(id));
    }

    @PostMapping("/lines")
    @Operation(summary = "Create line")
    public ResponseEntity<LineResponse> createLine(@Valid @RequestBody LineRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(routingCrudService.createLine(request));
    }

    @PutMapping("/lines/{id}")
    @Operation(summary = "Update line")
    public ResponseEntity<LineResponse> updateLine(@PathVariable Integer id, @Valid @RequestBody LineRequest request) {
        return ResponseEntity.ok(routingCrudService.updateLine(id, request));
    }

    @DeleteMapping("/lines/{id}")
    @Operation(summary = "Delete line")
    public ResponseEntity<Void> deleteLine(@PathVariable Integer id) {
        routingCrudService.deleteLine(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/directions")
    @Operation(summary = "List directions")
    public ResponseEntity<List<DirectionResponse>> getDirections(
            @RequestParam(required = false) Boolean activeOnly,
            @RequestParam(required = false) Integer lineId
    ) {
        return ResponseEntity.ok(routingCrudService.getDirections(activeOnly, lineId));
    }

    @GetMapping("/directions/{id}")
    @Operation(summary = "Get direction by id")
    public ResponseEntity<DirectionResponse> getDirection(@PathVariable Integer id) {
        return ResponseEntity.ok(routingCrudService.getDirection(id));
    }

    @PostMapping("/directions")
    @Operation(summary = "Create direction")
    public ResponseEntity<DirectionResponse> createDirection(@Valid @RequestBody DirectionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(routingCrudService.createDirection(request));
    }

    @PutMapping("/directions/{id}")
    @Operation(summary = "Update direction")
    public ResponseEntity<DirectionResponse> updateDirection(@PathVariable Integer id, @Valid @RequestBody DirectionRequest request) {
        return ResponseEntity.ok(routingCrudService.updateDirection(id, request));
    }

    @DeleteMapping("/directions/{id}")
    @Operation(summary = "Delete direction")
    public ResponseEntity<Void> deleteDirection(@PathVariable Integer id) {
        routingCrudService.deleteDirection(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/stations")
    @Operation(summary = "List stations")
    public ResponseEntity<List<StationResponse>> getStations(
            @RequestParam(required = false) Boolean activeOnly,
            @RequestParam(required = false) String name
    ) {
        return ResponseEntity.ok(routingCrudService.getStations(activeOnly, name));
    }

    @GetMapping("/stations/{id}")
    @Operation(summary = "Get station by id")
    public ResponseEntity<StationResponse> getStation(@PathVariable Integer id) {
        return ResponseEntity.ok(routingCrudService.getStation(id));
    }

    @PostMapping("/stations")
    @Operation(summary = "Create station")
    public ResponseEntity<StationResponse> createStation(@Valid @RequestBody StationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(routingCrudService.createStation(request));
    }

    @PutMapping("/stations/{id}")
    @Operation(summary = "Update station")
    public ResponseEntity<StationResponse> updateStation(@PathVariable Integer id, @Valid @RequestBody StationRequest request) {
        return ResponseEntity.ok(routingCrudService.updateStation(id, request));
    }

    @DeleteMapping("/stations/{id}")
    @Operation(summary = "Delete station")
    public ResponseEntity<Void> deleteStation(@PathVariable Integer id) {
        routingCrudService.deleteStation(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/timetables")
    @Operation(summary = "List timetables")
    public ResponseEntity<List<TimetableResponse>> getTimetables(
            @RequestParam(required = false) Integer lineId,
            @RequestParam(required = false) Integer directionId,
            @RequestParam(required = false) Boolean activeOnly
    ) {
        return ResponseEntity.ok(routingCrudService.getTimetables(lineId, directionId, activeOnly));
    }

    @GetMapping("/timetables/{id}")
    @Operation(summary = "Get timetable by id")
    public ResponseEntity<TimetableResponse> getTimetable(@PathVariable Integer id) {
        return ResponseEntity.ok(routingCrudService.getTimetable(id));
    }

    @PostMapping("/timetables")
    @Operation(summary = "Create timetable")
    public ResponseEntity<TimetableResponse> createTimetable(@Valid @RequestBody TimetableRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(routingCrudService.createTimetable(request));
    }

    @PutMapping("/timetables/{id}")
    @Operation(summary = "Update timetable")
    public ResponseEntity<TimetableResponse> updateTimetable(@PathVariable Integer id, @Valid @RequestBody TimetableRequest request) {
        return ResponseEntity.ok(routingCrudService.updateTimetable(id, request));
    }

    @DeleteMapping("/timetables/{id}")
    @Operation(summary = "Delete timetable")
    public ResponseEntity<Void> deleteTimetable(@PathVariable Integer id) {
        routingCrudService.deleteTimetable(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/directions/{directionId}/stations")
    @Operation(summary = "List stations for direction")
    public ResponseEntity<List<DirectionStationResponse>> getDirectionStations(@PathVariable Integer directionId) {
        return ResponseEntity.ok(routingCrudService.getDirectionStationsByDirection(directionId));
    }

    @PostMapping("/direction-stations")
    @Operation(summary = "Create direction station relation")
    public ResponseEntity<DirectionStationResponse> createDirectionStation(@Valid @RequestBody DirectionStationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(routingCrudService.createDirectionStation(request));
    }

    @PutMapping("/direction-stations/{id}")
    @Operation(summary = "Update direction station relation")
    public ResponseEntity<DirectionStationResponse> updateDirectionStation(@PathVariable Integer id, @Valid @RequestBody DirectionStationRequest request) {
        return ResponseEntity.ok(routingCrudService.updateDirectionStation(id, request));
    }

    @DeleteMapping("/direction-stations/{id}")
    @Operation(summary = "Delete direction station relation")
    public ResponseEntity<Void> deleteDirectionStation(@PathVariable Integer id) {
        routingCrudService.deleteDirectionStation(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/directions/{directionId}/route-points")
    @Operation(summary = "List route points for direction")
    public ResponseEntity<List<RoutePointResponse>> getRoutePoints(@PathVariable Integer directionId) {
        return ResponseEntity.ok(routingCrudService.getRoutePointsByDirection(directionId));
    }

    @GetMapping("/directions/{directionId}/geojson")
    @Operation(summary = "Get direction route as GeoJSON Feature")
    public ResponseEntity<GeoJsonFeatureResponse> getDirectionGeoJson(@PathVariable Integer directionId) {
        return ResponseEntity.ok(routingCrudService.getDirectionGeoJson(directionId));
    }

    @PostMapping("/route-points")
    @Operation(summary = "Create route point")
    public ResponseEntity<RoutePointResponse> createRoutePoint(@Valid @RequestBody RoutePointRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(routingCrudService.createRoutePoint(request));
    }

    @PutMapping("/route-points/{id}")
    @Operation(summary = "Update route point")
    public ResponseEntity<RoutePointResponse> updateRoutePoint(@PathVariable Integer id, @Valid @RequestBody RoutePointRequest request) {
        return ResponseEntity.ok(routingCrudService.updateRoutePoint(id, request));
    }

    @DeleteMapping("/route-points/{id}")
    @Operation(summary = "Delete route point")
    public ResponseEntity<Void> deleteRoutePoint(@PathVariable Integer id) {
        routingCrudService.deleteRoutePoint(id);
        return ResponseEntity.noContent().build();
    }
}
