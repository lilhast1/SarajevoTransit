package ba.unsa.etf.pnwt.routingservice.importer;

import ba.unsa.etf.pnwt.routingservice.repository.DirectionRepository;
import ba.unsa.etf.pnwt.routingservice.repository.LineRepository;
import ba.unsa.etf.pnwt.routingservice.repository.StationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
@Order(2)
@ConditionalOnProperty(prefix = "routing.import", name = "enabled", havingValue = "true")
public class RoutingSnapshotImportRunner implements CommandLineRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(RoutingSnapshotImportRunner.class);

    private final RoutingSnapshotImporter importer;
    private final LineRepository lineRepository;
    private final DirectionRepository directionRepository;
    private final StationRepository stationRepository;

    @Value("${routing.import.file}")
    private String importFile;

    public RoutingSnapshotImportRunner(
            RoutingSnapshotImporter importer,
            LineRepository lineRepository,
            DirectionRepository directionRepository,
            StationRepository stationRepository
    ) {
        this.importer = importer;
        this.lineRepository = lineRepository;
        this.directionRepository = directionRepository;
        this.stationRepository = stationRepository;
    }

    @Override
    public void run(String... args) {
        long lines = lineRepository.count();
        long directions = directionRepository.count();
        long stations = stationRepository.count();
        LOGGER.info("Routing import check started. importFile={}, lines={}, directions={}, stations={}", importFile, lines, directions, stations);

        if (lines > 0 || directions > 0 || stations > 0) {
            LOGGER.info("Routing import skipped because routing data already exists in database.");
            return;
        }

        Path resolvedImportFile = resolvePath(importFile);
        if (!Files.exists(resolvedImportFile)) {
            throw new IllegalStateException("Routing import file not found: " + resolvedImportFile);
        }

        LOGGER.info("Routing database appears empty. Starting first-time snapshot import from {}", resolvedImportFile);
        long startedAt = System.currentTimeMillis();
        ImportSummary summary = importer.importSnapshot(importFile);
        long durationMs = System.currentTimeMillis() - startedAt;
        LOGGER.info("Routing import finished. lines={}, directions={}, stations={}, directionStations={}, routePoints={}, timetables={}, skippedTimetables={}",
                summary.getLinesProcessed(),
                summary.getDirectionsProcessed(),
                summary.getStationsProcessed(),
                summary.getDirectionStationsProcessed(),
                summary.getRoutePointsProcessed(),
                summary.getTimetablesProcessed(),
                summary.getTimetableEntriesSkipped());
        LOGGER.info("Routing first-time import completed in {} ms.", durationMs);

        if (!summary.getMissingTimetableDirections().isEmpty()) {
            LOGGER.warn("Directions without timetable: {}", summary.getMissingTimetableDirections());
        }
        for (String warning : summary.getWarnings()) {
            LOGGER.warn("Import warning: {}", warning);
        }
    }

    private Path resolvePath(String filePath) {
        Path path = Paths.get(filePath);
        if (path.isAbsolute()) {
            return path.normalize();
        }
        return Paths.get("").toAbsolutePath().resolve(path).normalize();
    }
}
