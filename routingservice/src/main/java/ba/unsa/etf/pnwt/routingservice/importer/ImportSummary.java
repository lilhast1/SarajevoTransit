package ba.unsa.etf.pnwt.routingservice.importer;

import java.util.ArrayList;
import java.util.List;

public class ImportSummary {

    private int linesProcessed;
    private int directionsProcessed;
    private int stationsProcessed;
    private int directionStationsProcessed;
    private int routePointsProcessed;
    private int timetablesProcessed;
    private int timetableEntriesSkipped;
    private final List<Integer> missingTimetableDirections = new ArrayList<>();
    private final List<String> warnings = new ArrayList<>();

    public int getLinesProcessed() {
        return linesProcessed;
    }

    public void incrementLinesProcessed() {
        this.linesProcessed++;
    }

    public int getDirectionsProcessed() {
        return directionsProcessed;
    }

    public void incrementDirectionsProcessed() {
        this.directionsProcessed++;
    }

    public int getStationsProcessed() {
        return stationsProcessed;
    }

    public void incrementStationsProcessed() {
        this.stationsProcessed++;
    }

    public int getDirectionStationsProcessed() {
        return directionStationsProcessed;
    }

    public void incrementDirectionStationsProcessed() {
        this.directionStationsProcessed++;
    }

    public int getRoutePointsProcessed() {
        return routePointsProcessed;
    }

    public void incrementRoutePointsProcessed() {
        this.routePointsProcessed++;
    }

    public int getTimetablesProcessed() {
        return timetablesProcessed;
    }

    public void incrementTimetablesProcessed() {
        this.timetablesProcessed++;
    }

    public int getTimetableEntriesSkipped() {
        return timetableEntriesSkipped;
    }

    public void incrementTimetableEntriesSkipped() {
        this.timetableEntriesSkipped++;
    }

    public List<Integer> getMissingTimetableDirections() {
        return missingTimetableDirections;
    }

    public void addMissingTimetableDirection(Integer directionId) {
        if (directionId != null && !missingTimetableDirections.contains(directionId)) {
            missingTimetableDirections.add(directionId);
        }
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public void addWarning(String warning) {
        warnings.add(warning);
    }
}
