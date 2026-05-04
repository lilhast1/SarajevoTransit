package ba.unsa.etf.pnwt.routingservice.importer;

import ba.unsa.etf.pnwt.routingservice.importer.model.ScrapeSnapshot;
import ba.unsa.etf.pnwt.routingservice.importer.model.ScrapedDirection;
import ba.unsa.etf.pnwt.routingservice.importer.model.ScrapedLine;
import ba.unsa.etf.pnwt.routingservice.importer.model.ScrapedRoutePoint;
import ba.unsa.etf.pnwt.routingservice.importer.model.ScrapedStation;
import ba.unsa.etf.pnwt.routingservice.importer.model.ScrapedTimetableEntry;
import ba.unsa.etf.pnwt.routingservice.importer.model.ScrapedTimetableGroup;
import ba.unsa.etf.pnwt.routingservice.model.Direction;
import ba.unsa.etf.pnwt.routingservice.model.DirectionStation;
import ba.unsa.etf.pnwt.routingservice.model.Line;
import ba.unsa.etf.pnwt.routingservice.model.RoutePoint;
import ba.unsa.etf.pnwt.routingservice.model.Station;
import ba.unsa.etf.pnwt.routingservice.model.Timetable;
import ba.unsa.etf.pnwt.routingservice.model.VehicleType;
import ba.unsa.etf.pnwt.routingservice.repository.DirectionRepository;
import ba.unsa.etf.pnwt.routingservice.repository.DirectionStationRepository;
import ba.unsa.etf.pnwt.routingservice.repository.LineRepository;
import ba.unsa.etf.pnwt.routingservice.repository.RoutePointRepository;
import ba.unsa.etf.pnwt.routingservice.repository.StationRepository;
import ba.unsa.etf.pnwt.routingservice.repository.TimetableRepository;
import ba.unsa.etf.pnwt.routingservice.repository.VehicleTypeRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

@Service
public class RoutingSnapshotImporter {

    private static final ZoneId SARAJEVO_TIMEZONE = ZoneId.of("Europe/Sarajevo");
    private static final int BATCH_SIZE = 500;

    private final ObjectMapper objectMapper;
    private final EntityManager entityManager;
    private final VehicleTypeRepository vehicleTypeRepository;
    private final LineRepository lineRepository;
    private final DirectionRepository directionRepository;
    private final StationRepository stationRepository;
    private final DirectionStationRepository directionStationRepository;
    private final RoutePointRepository routePointRepository;
    private final TimetableRepository timetableRepository;

    public RoutingSnapshotImporter(
            ObjectMapper objectMapper,
            EntityManager entityManager,
            VehicleTypeRepository vehicleTypeRepository,
            LineRepository lineRepository,
            DirectionRepository directionRepository,
            StationRepository stationRepository,
            DirectionStationRepository directionStationRepository,
            RoutePointRepository routePointRepository,
            TimetableRepository timetableRepository
    ) {
        this.objectMapper = objectMapper;
        this.entityManager = entityManager;
        this.vehicleTypeRepository = vehicleTypeRepository;
        this.lineRepository = lineRepository;
        this.directionRepository = directionRepository;
        this.stationRepository = stationRepository;
        this.directionStationRepository = directionStationRepository;
        this.routePointRepository = routePointRepository;
        this.timetableRepository = timetableRepository;
    }

    @Transactional
    public ImportSummary importSnapshot(String filePath) {
        ScrapeSnapshot snapshot = readSnapshot(filePath);
        ImportSummary summary = new ImportSummary();

        lineRepository.deactivateAll();
        directionRepository.deactivateAll();
        stationRepository.deactivateAll();
        timetableRepository.deactivateAll();

        Map<Integer, Line> importedLines = new HashMap<>();
        Map<Integer, Direction> importedDirections = new HashMap<>();
        Map<Integer, Set<Integer>> directionIdsByLine = new HashMap<>();

        List<Integer> lineExternalIds = snapshot.getLines().stream()
                .map(ScrapedLine::getId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Integer, Line> existingLines = fetchLinesByExternalIds(lineExternalIds);

        List<Integer> directionExternalIds = snapshot.getLines().stream()
                .flatMap(line -> safeList(line.getDirections()).stream())
                .map(ScrapedDirection::getTerminusLineId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Integer, Direction> existingDirections = fetchDirectionsByExternalIds(directionExternalIds);

        List<Integer> stationExternalIds = snapshot.getLines().stream()
                .flatMap(line -> safeList(line.getDirections()).stream())
                .flatMap(direction -> safeList(direction.getStations()).stream())
                .map(ScrapedStation::getId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Integer, Station> existingStations = fetchStationsByExternalIds(stationExternalIds);

        Map<Integer, ScrapedStation> scrapedStationsById = new HashMap<>();
        for (ScrapedLine line : snapshot.getLines()) {
            for (ScrapedDirection direction : safeList(line.getDirections())) {
                for (ScrapedStation scrapedStation : safeList(direction.getStations())) {
                    if (scrapedStation.getId() != null
                            && scrapedStation.getLatitude() != null
                            && scrapedStation.getLongitude() != null) {
                        scrapedStationsById.put(scrapedStation.getId(), scrapedStation);
                    }
                }
            }
        }

        List<Station> stationsToSave = new ArrayList<>();
        for (ScrapedStation scrapedStation : scrapedStationsById.values()) {
            Station station = existingStations.getOrDefault(scrapedStation.getId(), new Station());
            station.setExternalId(scrapedStation.getId());
            station.setCode(scrapedStation.getCode());
            station.setName(scrapedStation.getName());
            station.setAddress(scrapedStation.getAddress());
            station.setLatitude(scrapedStation.getLatitude());
            station.setLongitude(scrapedStation.getLongitude());
            station.setIsActive(true);
            stationsToSave.add(station);
            existingStations.put(station.getExternalId(), station);
            summary.incrementStationsProcessed();
        }

        saveInChunks(stationsToSave, stationRepository::saveAll);

        List<Integer> existingDirectionIdsToClear = existingDirections.values().stream()
                .map(Direction::getId)
                .filter(Objects::nonNull)
                .toList();
        if (!existingDirectionIdsToClear.isEmpty()) {
            directionStationRepository.deleteByDirection_IdIn(existingDirectionIdsToClear);
            routePointRepository.deleteByDirection_IdIn(existingDirectionIdsToClear);
        }
        List<Integer> existingLineIdsToClear = existingLines.values().stream()
                .map(Line::getId)
                .filter(Objects::nonNull)
                .toList();
        if (!existingLineIdsToClear.isEmpty()) {
            timetableRepository.deleteByLine_IdIn(existingLineIdsToClear);
        }

        List<Line> linesToSave = new ArrayList<>();
        List<Direction> directionsToSave = new ArrayList<>();
        List<RoutePoint> routePointsToSave = new ArrayList<>();
        List<DirectionStation> directionStationsToSave = new ArrayList<>();
        List<Timetable> timetablesToSave = new ArrayList<>();

        for (ScrapedLine scrapedLine : snapshot.getLines()) {
            Line line = upsertLine(scrapedLine, existingLines, summary);
            if (line == null) {
                continue;
            }

            existingLines.put(line.getExternalId(), line);
            linesToSave.add(line);

            importedLines.put(line.getExternalId(), line);
            summary.incrementLinesProcessed();

            for (ScrapedDirection scrapedDirection : safeList(scrapedLine.getDirections())) {
                Direction direction = upsertDirection(line, scrapedDirection, existingDirections, summary);
                if (direction == null) {
                    continue;
                }

                existingDirections.put(direction.getExternalId(), direction);
                directionsToSave.add(direction);

                importedDirections.put(direction.getExternalId(), direction);
                directionIdsByLine.computeIfAbsent(line.getExternalId(), key -> new HashSet<>()).add(direction.getExternalId());
                summary.incrementDirectionsProcessed();

                List<DirectionStation> directionStations = rebuildDirectionStations(direction, scrapedDirection, existingStations, summary);
                List<RoutePoint> routePoints = buildRoutePoints(direction, scrapedDirection, summary);
                interpolateTravelTimes(directionStations, scrapedDirection.getRoutePoints(), line.getVehicleType());
                directionStationsToSave.addAll(directionStations);
                routePointsToSave.addAll(routePoints);
            }
        }

        saveInChunks(linesToSave, lineRepository::saveAll);

        for (Line savedLine : linesToSave) {
            if (savedLine.getExternalId() != null) {
                importedLines.put(savedLine.getExternalId(), savedLine);
            }
        }

        saveInChunks(directionsToSave, directionRepository::saveAll);

        for (Direction savedDirection : directionsToSave) {
            if (savedDirection.getExternalId() != null) {
                importedDirections.put(savedDirection.getExternalId(), savedDirection);
            }
        }

        saveInChunks(routePointsToSave, routePointRepository::saveAll);
        saveInChunks(directionStationsToSave, directionStationRepository::saveAll);

        for (ScrapedLine scrapedLine : snapshot.getLines()) {
            Line importedLine = importedLines.get(scrapedLine.getId());
            if (importedLine == null) {
                continue;
            }
            importTimetables(importedLine, scrapedLine, importedDirections, directionIdsByLine, timetablesToSave, summary);
        }

        saveInChunks(timetablesToSave, timetableRepository::saveAll);

        return summary;
    }

    private ScrapeSnapshot readSnapshot(String filePath) {
        Path path = resolvePath(filePath);
        if (!Files.exists(path)) {
            throw new IllegalArgumentException("Import file does not exist: " + path);
        }

        try {
            return objectMapper.readValue(path.toFile(), ScrapeSnapshot.class);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read snapshot file: " + path, ex);
        }
    }

    private Path resolvePath(String filePath) {
        Path path = Paths.get(filePath);
        if (path.isAbsolute()) {
            return path.normalize();
        }
        return Paths.get("").toAbsolutePath().resolve(path).normalize();
    }

    private Line upsertLine(ScrapedLine scrapedLine, Map<Integer, Line> existingLines, ImportSummary summary) {
        if (scrapedLine.getId() == null) {
            summary.addWarning("Skipped line with missing id");
            return null;
        }
        if (scrapedLine.getVehicleTypeId() == null) {
            summary.addWarning("Skipped line " + scrapedLine.getId() + " due to missing vehicleTypeId");
            return null;
        }

        Optional<VehicleType> vehicleTypeOptional = vehicleTypeRepository.findById(scrapedLine.getVehicleTypeId());
        if (vehicleTypeOptional.isEmpty()) {
            summary.addWarning("Skipped line " + scrapedLine.getId() + " due to unknown vehicleTypeId=" + scrapedLine.getVehicleTypeId());
            return null;
        }

        Line line = existingLines.getOrDefault(scrapedLine.getId(), new Line());
        line.setExternalId(scrapedLine.getId());
        line.setCode(scrapedLine.getCode());
        line.setName(scrapedLine.getName());
        line.setVehicleType(vehicleTypeOptional.get());
        line.setIsActive(true);
        return line;
    }

    private Direction upsertDirection(Line line, ScrapedDirection scrapedDirection, Map<Integer, Direction> existingDirections, ImportSummary summary) {
        Integer directionId = scrapedDirection.getTerminusLineId();
        if (directionId == null) {
            summary.addWarning("Skipped direction on line " + line.getExternalId() + " due to missing terminusLineId");
            return null;
        }

        Direction direction = existingDirections.getOrDefault(directionId, new Direction());
        direction.setExternalId(directionId);
        direction.setLine(line);
        direction.setCode(scrapedDirection.getCode());
        direction.setName(scrapedDirection.getName());
        direction.setDirectionLabel(scrapedDirection.getDirectionLabel());
        direction.setLengthMeters(scrapedDirection.getLengthMeters() == null ? 0D : scrapedDirection.getLengthMeters());
        direction.setCanDelete(Boolean.TRUE.equals(scrapedDirection.getCanDelete()));
        direction.setIsActive(true);
        return direction;
    }

    private List<DirectionStation> rebuildDirectionStations(
            Direction direction,
            ScrapedDirection scrapedDirection,
            Map<Integer, Station> existingStations,
            ImportSummary summary
    ) {
        List<DirectionStation> directionStations = new ArrayList<>();
            int sequence = 1;
        for (ScrapedStation scrapedStation : safeList(scrapedDirection.getStations())) {
            if (scrapedStation.getId() == null || scrapedStation.getLatitude() == null || scrapedStation.getLongitude() == null) {
                summary.addWarning("Skipped station for direction " + direction.getId() + " due to missing id/coordinates");
                continue;
            }

            Station station = existingStations.get(scrapedStation.getId());
            if (station == null) {
                summary.addWarning("Skipped station relation for direction " + direction.getId() + " because station was not imported: " + scrapedStation.getId());
                continue;
            }

            DirectionStation directionStation = new DirectionStation();
            directionStation.setDirection(direction);
            directionStation.setStation(station);
            directionStation.setStopSequence(sequence++);
            directionStation.setTravelTimeFromPrevSeconds(null);
            directionStations.add(directionStation);
            summary.incrementDirectionStationsProcessed();
        }

        return directionStations;
    }

    private List<RoutePoint> buildRoutePoints(Direction direction, ScrapedDirection scrapedDirection, ImportSummary summary) {
        List<RoutePoint> routePoints = new ArrayList<>();
        int sequence = 1;
        for (ScrapedRoutePoint scrapedRoutePoint : safeList(scrapedDirection.getRoutePoints())) {
            if (scrapedRoutePoint.getLatitude() == null || scrapedRoutePoint.getLongitude() == null) {
                continue;
            }

            RoutePoint routePoint = new RoutePoint();
            routePoint.setDirection(direction);
            routePoint.setSegmentId(scrapedRoutePoint.getId());
            routePoint.setSequenceOrder(sequence++);
            routePoint.setLatitude(scrapedRoutePoint.getLatitude());
            routePoint.setLongitude(scrapedRoutePoint.getLongitude());
            routePoints.add(routePoint);
            summary.incrementRoutePointsProcessed();
        }

        return routePoints;
    }

    private void importTimetables(
            Line line,
            ScrapedLine scrapedLine,
            Map<Integer, Direction> importedDirections,
            Map<Integer, Set<Integer>> directionIdsByLine,
            List<Timetable> timetablesToSave,
            ImportSummary summary
    ) {
        Set<Integer> directionsWithTimetable = new HashSet<>();
        for (ScrapedTimetableGroup group : safeList(scrapedLine.getTimetableGroups())) {
            Integer directionId = group.getId();
            if (directionId == null) {
                summary.addWarning("Skipped timetable group on line " + line.getExternalId() + " due to missing group id");
                continue;
            }

            Direction direction = importedDirections.get(directionId);
            if (direction == null) {
                summary.addWarning("Skipped timetable group for unknown direction " + directionId + " on line " + line.getExternalId());
                continue;
            }

            List<ScrapedTimetableEntry> entries = safeList(group.getTimetable());
            if (entries.isEmpty()) {
                summary.addMissingTimetableDirection(directionId);
                continue;
            }

            directionsWithTimetable.add(directionId);

            for (ScrapedTimetableEntry entry : entries) {
                if (entry.getId() == null || entry.getTerminusLineId() == null || entry.getLineId() == null) {
                    summary.incrementTimetableEntriesSkipped();
                    summary.addWarning("Skipped timetable entry due to missing ids in line " + line.getExternalId());
                    continue;
                }
                if (!Objects.equals(entry.getLineId(), line.getExternalId())) {
                    summary.incrementTimetableEntriesSkipped();
                    summary.addWarning("Skipped timetable " + entry.getId() + " due to line mismatch. expected=" + line.getExternalId() + ", actual=" + entry.getLineId());
                    continue;
                }
                if (!Objects.equals(entry.getTerminusLineId(), direction.getExternalId())) {
                    summary.incrementTimetableEntriesSkipped();
                    summary.addWarning("Skipped timetable " + entry.getId() + " due to direction mismatch. expected=" + direction.getExternalId() + ", actual=" + entry.getTerminusLineId());
                    continue;
                }

                LocalTime departureTime;
                if (entry.getStartTime() == null || entry.getStartTime().isBlank()) {
                    summary.incrementTimetableEntriesSkipped();
                    summary.addWarning("Skipped timetable " + entry.getId() + " due to missing start_time");
                    continue;
                }

                try {
                    departureTime = LocalTime.parse(entry.getStartTime());
                } catch (DateTimeParseException ex) {
                    summary.incrementTimetableEntriesSkipped();
                    summary.addWarning("Skipped timetable " + entry.getId() + " due to invalid start_time=" + entry.getStartTime());
                    continue;
                }

                Timetable timetable = new Timetable();
                timetable.setExternalId(entry.getId());
                timetable.setName(entry.getName());
                timetable.setDirection(direction);
                timetable.setLine(line);
                timetable.setDepartureTime(departureTime);
                timetable.setValidFrom(parseIsoToSarajevoDate(entry.getValidFrom()));
                timetable.setValidTo(parseIsoToSarajevoDate(entry.getValidTo()));
                timetable.setRidesOnHolidays(Boolean.TRUE.equals(entry.getRidesOnHolidays()));
                List<Short> days = entry.getDaysOfWeek() == null ? List.of() : entry.getDaysOfWeek();
                timetable.setDaysOfWeek(days.toArray(new Short[0]));
                timetable.setReceivesPassengers(!Boolean.FALSE.equals(entry.getReceivesPassengers()));
                timetable.setIsActive(true);

                timetablesToSave.add(timetable);
                summary.incrementTimetablesProcessed();
            }
        }

        Set<Integer> allDirectionsForLine = directionIdsByLine.getOrDefault(line.getExternalId(), Set.of());
        for (Integer directionId : allDirectionsForLine) {
            if (!directionsWithTimetable.contains(directionId)) {
                summary.addMissingTimetableDirection(directionId);
            }
        }
    }

    private void interpolateTravelTimes(
            List<DirectionStation> directionStations,
            List<ScrapedRoutePoint> routePoints,
            VehicleType vehicleType
    ) {
        if (directionStations.isEmpty()) {
            return;
        }

        List<ScrapedRoutePoint> validRoutePoints = routePoints == null
                ? List.of()
                : routePoints.stream()
                .filter(point -> point.getLatitude() != null && point.getLongitude() != null)
                .toList();

        directionStations.sort(Comparator.comparing(DirectionStation::getStopSequence));
        directionStations.get(0).setTravelTimeFromPrevSeconds(null);

        if (directionStations.size() == 1) {
            return;
        }

        double[] segmentDistances = calculateSegmentDistances(directionStations, validRoutePoints);
        double speedMps = speedByVehicleType(vehicleType == null ? null : vehicleType.getId());
        int dwellSeconds = 25;

        for (int i = 1; i < directionStations.size(); i++) {
            double meters = segmentDistances[i - 1];
            int movingSeconds = (int) Math.round(meters / speedMps);
            int totalSeconds = Math.max(30, movingSeconds + dwellSeconds);
            directionStations.get(i).setTravelTimeFromPrevSeconds(totalSeconds);
        }
    }

    private double[] calculateSegmentDistances(List<DirectionStation> directionStations, List<ScrapedRoutePoint> routePoints) {
        int segments = directionStations.size() - 1;
        double[] segmentDistances = new double[segments];

        if (routePoints.size() < 2) {
            for (int i = 0; i < segments; i++) {
                segmentDistances[i] = 600D;
            }
            return segmentDistances;
        }

        double[] cumulative = new double[routePoints.size()];
        for (int i = 1; i < routePoints.size(); i++) {
            ScrapedRoutePoint prev = routePoints.get(i - 1);
            ScrapedRoutePoint curr = routePoints.get(i);
            cumulative[i] = cumulative[i - 1] + haversineMeters(
                    prev.getLatitude().doubleValue(),
                    prev.getLongitude().doubleValue(),
                    curr.getLatitude().doubleValue(),
                    curr.getLongitude().doubleValue()
            );
        }

        double[] stationPositions = new double[directionStations.size()];
        int fromIndex = 0;
        for (int i = 0; i < directionStations.size(); i++) {
            Station station = directionStations.get(i).getStation();
            int nearestIndex = nearestRoutePointIndex(
                    station.getLatitude().doubleValue(),
                    station.getLongitude().doubleValue(),
                    routePoints,
                    fromIndex
            );
            stationPositions[i] = cumulative[nearestIndex];
            fromIndex = nearestIndex;
        }

        boolean hasProgress = false;
        for (int i = 1; i < stationPositions.length; i++) {
            double diff = stationPositions[i] - stationPositions[i - 1];
            if (diff > 1) {
                hasProgress = true;
            }
            segmentDistances[i - 1] = Math.max(0, diff);
        }

        if (!hasProgress) {
            for (int i = 0; i < segments; i++) {
                segmentDistances[i] = 600D;
            }
        }

        return segmentDistances;
    }

    private int nearestRoutePointIndex(double latitude, double longitude, List<ScrapedRoutePoint> routePoints, int fromIndex) {
        int bestIndex = fromIndex;
        double bestDistance = Double.MAX_VALUE;

        for (int i = fromIndex; i < routePoints.size(); i++) {
            ScrapedRoutePoint routePoint = routePoints.get(i);
            double distance = haversineMeters(
                    latitude,
                    longitude,
                    routePoint.getLatitude().doubleValue(),
                    routePoint.getLongitude().doubleValue()
            );
            if (distance < bestDistance) {
                bestDistance = distance;
                bestIndex = i;
            }
        }

        return bestIndex;
    }

    private double speedByVehicleType(Short vehicleTypeId) {
        if (vehicleTypeId == null) {
            return 6.0D;
        }
        return switch (vehicleTypeId) {
            case 1 -> 6.8D;
            case 2 -> 6.2D;
            case 3 -> 6.0D;
            case 4 -> 7.5D;
            default -> 6.0D;
        };
    }

    private LocalDate parseIsoToSarajevoDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return Instant.parse(value).atZone(SARAJEVO_TIMEZONE).toLocalDate();
        } catch (DateTimeParseException ignored) {
        }

        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    private double haversineMeters(double lat1, double lon1, double lat2, double lon2) {
        double earthRadius = 6_371_000D;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadius * c;
    }

    private <T> List<T> safeList(List<T> list) {
        return list == null ? List.of() : list;
    }

    private Map<Integer, Line> fetchLinesByExternalIds(List<Integer> externalIds) {
        Map<Integer, Line> map = new HashMap<>();
        if (externalIds.isEmpty()) {
            return map;
        }
        for (Line line : lineRepository.findByExternalIdIn(externalIds)) {
            map.put(line.getExternalId(), line);
        }
        return map;
    }

    private Map<Integer, Direction> fetchDirectionsByExternalIds(List<Integer> externalIds) {
        Map<Integer, Direction> map = new HashMap<>();
        if (externalIds.isEmpty()) {
            return map;
        }
        for (Direction direction : directionRepository.findByExternalIdIn(externalIds)) {
            map.put(direction.getExternalId(), direction);
        }
        return map;
    }

    private Map<Integer, Station> fetchStationsByExternalIds(List<Integer> externalIds) {
        Map<Integer, Station> map = new HashMap<>();
        if (externalIds.isEmpty()) {
            return map;
        }
        for (Station station : stationRepository.findByExternalIdIn(externalIds)) {
            map.put(station.getExternalId(), station);
        }
        return map;
    }

    private <T> void saveInChunks(List<T> entities, Consumer<List<T>> saver) {
        if (entities.isEmpty()) {
            return;
        }

        int start = 0;
        while (start < entities.size()) {
            int end = Math.min(start + BATCH_SIZE, entities.size());
            saver.accept(entities.subList(start, end));
            entityManager.flush();
            entityManager.clear();
            start = end;
        }
    }
}
