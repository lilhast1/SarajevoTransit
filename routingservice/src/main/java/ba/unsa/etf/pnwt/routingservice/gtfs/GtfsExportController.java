package ba.unsa.etf.pnwt.routingservice.gtfs;

import ba.unsa.etf.pnwt.routingservice.exception.ForbiddenException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/gtfs")
@Tag(name = "GTFS Export", description = "Admin endpoints for GTFS generation")
public class GtfsExportController {

    private static final Logger LOGGER = LoggerFactory.getLogger(GtfsExportController.class);

    private final GtfsExportService gtfsExportService;

    @Value("${routing.gtfs.admin-token:}")
    private String adminToken;

    public GtfsExportController(GtfsExportService gtfsExportService) {
        this.gtfsExportService = gtfsExportService;
    }

    @PostMapping("/export")
    @Operation(summary = "Generate GTFS zip from current routing data")
    public ResponseEntity<byte[]> export(
            @RequestHeader(value = "X-Admin-Token", required = false) String requestToken,
            HttpServletRequest request
    ) {
        authorize(requestToken);

        GtfsExportResult result = gtfsExportService.export();
        if (!result.warnings().isEmpty()) {
            LOGGER.warn("GTFS export completed with {} warning(s)", result.warnings().size());
            for (String warning : result.warnings()) {
                LOGGER.warn("GTFS export warning: {}", warning);
            }
        }

        LOGGER.info(
                "GTFS export successful. file={}, routes={}, trips={}, stopTimes={}, shapes={}, fromIp={}",
                result.fileName(),
                result.routesCount(),
                result.tripsCount(),
                result.stopTimesCount(),
                result.shapesCount(),
                request.getRemoteAddr()
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/zip"));
        headers.setContentDisposition(ContentDisposition.attachment().filename(result.fileName()).build());
        headers.setContentLength(result.zipBytes().length);
        headers.set("X-GTFS-Routes", String.valueOf(result.routesCount()));
        headers.set("X-GTFS-Trips", String.valueOf(result.tripsCount()));
        headers.set("X-GTFS-StopTimes", String.valueOf(result.stopTimesCount()));
        headers.set("X-GTFS-Shapes", String.valueOf(result.shapesCount()));
        headers.set("X-GTFS-Warnings", String.valueOf(result.warnings().size()));

        return ResponseEntity.ok().headers(headers).body(result.zipBytes());
    }

    private void authorize(String requestToken) {
        if (adminToken == null || adminToken.isBlank()) {
            throw new IllegalStateException("GTFS admin token is not configured");
        }
        if (!adminToken.equals(requestToken)) {
            throw new ForbiddenException("Invalid admin token");
        }
    }
}
