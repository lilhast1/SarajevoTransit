package ba.unsa.etf.pnwt.routingservice.importer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(2)
@ConditionalOnProperty(prefix = "routing.import", name = "enabled", havingValue = "true")
public class RoutingSnapshotImportRunner implements CommandLineRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(RoutingSnapshotImportRunner.class);

    private final RoutingSnapshotImporter importer;

    @Value("${routing.import.file}")
    private String importFile;

    public RoutingSnapshotImportRunner(RoutingSnapshotImporter importer) {
        this.importer = importer;
    }

    @Override
    public void run(String... args) {
        ImportSummary summary = importer.importSnapshot(importFile);
        LOGGER.info("Routing import finished. lines={}, directions={}, stations={}, directionStations={}, routePoints={}, timetables={}, skippedTimetables={}",
                summary.getLinesProcessed(),
                summary.getDirectionsProcessed(),
                summary.getStationsProcessed(),
                summary.getDirectionStationsProcessed(),
                summary.getRoutePointsProcessed(),
                summary.getTimetablesProcessed(),
                summary.getTimetableEntriesSkipped());

        if (!summary.getMissingTimetableDirections().isEmpty()) {
            LOGGER.warn("Directions without timetable: {}", summary.getMissingTimetableDirections());
        }
        for (String warning : summary.getWarnings()) {
            LOGGER.warn("Import warning: {}", warning);
        }
    }
}
