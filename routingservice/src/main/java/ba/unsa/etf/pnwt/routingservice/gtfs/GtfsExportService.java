package ba.unsa.etf.pnwt.routingservice.gtfs;

import ba.unsa.etf.pnwt.routingservice.model.Direction;
import ba.unsa.etf.pnwt.routingservice.model.DirectionStation;
import ba.unsa.etf.pnwt.routingservice.model.Line;
import ba.unsa.etf.pnwt.routingservice.model.RoutePoint;
import ba.unsa.etf.pnwt.routingservice.model.Station;
import ba.unsa.etf.pnwt.routingservice.model.Timetable;
import ba.unsa.etf.pnwt.routingservice.repository.DirectionRepository;
import ba.unsa.etf.pnwt.routingservice.repository.DirectionStationRepository;
import ba.unsa.etf.pnwt.routingservice.repository.LineRepository;
import ba.unsa.etf.pnwt.routingservice.repository.RoutePointRepository;
import ba.unsa.etf.pnwt.routingservice.repository.StationRepository;
import ba.unsa.etf.pnwt.routingservice.repository.TimetableRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class GtfsExportService {

    private static final DateTimeFormatter GTFS_DATE = DateTimeFormatter.BASIC_ISO_DATE;
    private static final DateTimeFormatter GTFS_TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final LineRepository lineRepository;
    private final DirectionRepository directionRepository;
    private final StationRepository stationRepository;
    private final TimetableRepository timetableRepository;
    private final DirectionStationRepository directionStationRepository;
    private final RoutePointRepository routePointRepository;

    public GtfsExportService(
            LineRepository lineRepository,
            DirectionRepository directionRepository,
            StationRepository stationRepository,
            TimetableRepository timetableRepository,
            DirectionStationRepository directionStationRepository,
            RoutePointRepository routePointRepository
    ) {
        this.lineRepository = lineRepository;
        this.directionRepository = directionRepository;
        this.stationRepository = stationRepository;
        this.timetableRepository = timetableRepository;
        this.directionStationRepository = directionStationRepository;
        this.routePointRepository = routePointRepository;
    }

    @Transactional(readOnly = true)
    public GtfsExportResult export() {
        List<String> warnings = new ArrayList<>();

        List<Line> lines = lineRepository.findByIsActiveTrue();
        List<Direction> directions = directionRepository.findByIsActiveTrue();
        List<Station> stations = stationRepository.findByIsActiveTrue();
        List<Timetable> timetables = timetableRepository.findByIsActiveTrueOrderByDepartureTimeAsc();

        Map<Integer, Direction> directionById = new HashMap<>();
        for (Direction direction : directions) {
            directionById.put(direction.getId(), direction);
        }

        List<Integer> directionIds = directions.stream()
                .map(Direction::getId)
                .toList();

        Map<Integer, List<DirectionStation>> stationsByDirection = groupDirectionStations(directionIds);
        Map<Integer, List<RoutePoint>> pointsByDirection = groupRoutePoints(directionIds);

        List<String[]> agencyRows = buildAgencyRows();
        List<String[]> stopsRows = buildStopsRows(stations);
        List<String[]> routesRows = buildRoutesRows(lines);

        CalendarBuild calendarBuild = buildCalendarRows(timetables);
        List<String[]> tripsRows = buildTripsRows(timetables, directionById, calendarBuild.serviceIdByTimetableId(), warnings);
        StopTimesBuild stopTimesBuild = buildStopTimesRows(timetables, stationsByDirection, warnings);
        List<String[]> shapesRows = buildShapesRows(pointsByDirection, warnings);
        List<String[]> feedInfoRows = buildFeedInfoRows();

        Map<String, String> files = new LinkedHashMap<>();
        files.put("agency.txt", toCsv(agencyRows));
        files.put("stops.txt", toCsv(stopsRows));
        files.put("routes.txt", toCsv(routesRows));
        files.put("calendar.txt", toCsv(calendarBuild.rows()));
        files.put("trips.txt", toCsv(tripsRows));
        files.put("stop_times.txt", toCsv(stopTimesBuild.rows()));
        files.put("shapes.txt", toCsv(shapesRows));
        files.put("feed_info.txt", toCsv(feedInfoRows));

        byte[] zip = zipFiles(files);
        String fileName = "routing-gtfs-" + OffsetDateTime.now().toEpochSecond() + ".zip";

        return new GtfsExportResult(
                fileName,
                zip,
                routesRows.size() - 1,
                tripsRows.size() - 1,
                stopTimesBuild.rows().size() - 1,
                shapesRows.size() - 1,
                warnings
        );
    }

    private List<String[]> buildAgencyRows() {
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"agency_id", "agency_name", "agency_url", "agency_timezone", "agency_lang"});
        rows.add(new String[]{"sarajevo-transit", "Sarajevo Transit", "https://javniprevozks.ba", "Europe/Sarajevo", "bs"});
        return rows;
    }

    private List<String[]> buildStopsRows(List<Station> stations) {
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"stop_id", "stop_name", "stop_lat", "stop_lon"});

        stations.stream()
                .sorted(Comparator.comparing(Station::getId))
                .forEach(station -> rows.add(new String[]{
                        "stop-" + station.getId(),
                        safe(station.getName()),
                        station.getLatitude().toPlainString(),
                        station.getLongitude().toPlainString()
                }));

        return rows;
    }

    private List<String[]> buildRoutesRows(List<Line> lines) {
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"route_id", "agency_id", "route_short_name", "route_long_name", "route_type"});

        lines.stream()
                .sorted(Comparator.comparing(Line::getId))
                .forEach(line -> rows.add(new String[]{
                        routeId(line.getId()),
                        "sarajevo-transit",
                        safe(line.getCode()),
                        safe(line.getName()),
                        String.valueOf(toGtfsRouteType(line.getVehicleType().getId()))
                }));

        return rows;
    }

    private CalendarBuild buildCalendarRows(List<Timetable> timetables) {
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{
                "service_id",
                "monday",
                "tuesday",
                "wednesday",
                "thursday",
                "friday",
                "saturday",
                "sunday",
                "start_date",
                "end_date"
        });

        Map<String, String> serviceIdByKey = new LinkedHashMap<>();
        Map<Integer, String> serviceIdByTimetableId = new HashMap<>();

        for (Timetable timetable : timetables) {
            String key = serviceKey(timetable);
            String serviceId = serviceIdByKey.get(key);
            if (serviceId == null) {
                serviceId = "svc-" + (serviceIdByKey.size() + 1);
                serviceIdByKey.put(key, serviceId);
                rows.add(calendarRow(serviceId, timetable));
            }
            serviceIdByTimetableId.put(timetable.getId(), serviceId);
        }

        return new CalendarBuild(rows, serviceIdByTimetableId);
    }

    private List<String[]> buildTripsRows(
            List<Timetable> timetables,
            Map<Integer, Direction> directionById,
            Map<Integer, String> serviceIdByTimetableId,
            List<String> warnings
    ) {
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"route_id", "service_id", "trip_id", "trip_headsign", "direction_id", "shape_id"});

        for (Timetable timetable : timetables) {
            Direction direction = directionById.get(timetable.getDirection().getId());
            if (direction == null) {
                warnings.add("Skipped trip for timetable id=" + timetable.getId() + " because direction is missing");
                continue;
            }

            String serviceId = serviceIdByTimetableId.get(timetable.getId());
            if (serviceId == null) {
                warnings.add("Skipped trip for timetable id=" + timetable.getId() + " because service_id is missing");
                continue;
            }

            rows.add(new String[]{
                    routeId(timetable.getLine().getId()),
                    serviceId,
                    tripId(timetable.getId()),
                    safe(direction.getName()),
                    String.valueOf(toDirectionId(direction)),
                    shapeId(direction.getId())
            });
        }

        return rows;
    }

    private StopTimesBuild buildStopTimesRows(
            List<Timetable> timetables,
            Map<Integer, List<DirectionStation>> stationsByDirection,
            List<String> warnings
    ) {
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"trip_id", "arrival_time", "departure_time", "stop_id", "stop_sequence"});

        int emitted = 0;
        for (Timetable timetable : timetables) {
            Integer directionId = timetable.getDirection().getId();
            List<DirectionStation> orderedStops = stationsByDirection.getOrDefault(directionId, List.of());
            if (orderedStops.isEmpty()) {
                warnings.add("Skipped stop_times for timetable id=" + timetable.getId() + " because no direction_stations found");
                continue;
            }

            LocalTime cursor = timetable.getDepartureTime();
            for (int i = 0; i < orderedStops.size(); i++) {
                DirectionStation ds = orderedStops.get(i);
                if (i > 0) {
                    int delta = ds.getTravelTimeFromPrevSeconds() == null ? 120 : ds.getTravelTimeFromPrevSeconds();
                    cursor = cursor.plusSeconds(Math.max(0, delta));
                }

                rows.add(new String[]{
                        tripId(timetable.getId()),
                        formatGtfsTime(cursor),
                        formatGtfsTime(cursor),
                        stopId(ds.getStation().getId()),
                        String.valueOf(i + 1)
                });
                emitted++;
            }
        }

        return new StopTimesBuild(rows, emitted);
    }

    private List<String[]> buildShapesRows(Map<Integer, List<RoutePoint>> pointsByDirection, List<String> warnings) {
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"shape_id", "shape_pt_lat", "shape_pt_lon", "shape_pt_sequence"});

        for (Map.Entry<Integer, List<RoutePoint>> entry : pointsByDirection.entrySet()) {
            Integer directionId = entry.getKey();
            List<RoutePoint> points = entry.getValue();
            if (points.isEmpty()) {
                warnings.add("Direction id=" + directionId + " has no route points, shape omitted");
                continue;
            }

            for (RoutePoint point : points) {
                rows.add(new String[]{
                        shapeId(directionId),
                        point.getLatitude().toPlainString(),
                        point.getLongitude().toPlainString(),
                        String.valueOf(point.getSequenceOrder())
                });
            }
        }

        return rows;
    }

    private List<String[]> buildFeedInfoRows() {
        LocalDate today = LocalDate.now();
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"feed_publisher_name", "feed_publisher_url", "feed_lang", "feed_start_date", "feed_end_date", "feed_version"});
        rows.add(new String[]{
                "Sarajevo Transit",
                "https://javniprevozks.ba",
                "bs",
                GTFS_DATE.format(today.minusDays(1)),
                GTFS_DATE.format(today.plusYears(1)),
                "v1"
        });
        return rows;
    }

    private Map<Integer, List<DirectionStation>> groupDirectionStations(Collection<Integer> directionIds) {
        Map<Integer, List<DirectionStation>> map = new HashMap<>();
        if (directionIds.isEmpty()) {
            return map;
        }
        List<DirectionStation> all = directionStationRepository.findByDirection_IdInOrderByDirection_IdAscStopSequenceAsc(directionIds);
        for (DirectionStation directionStation : all) {
            map.computeIfAbsent(directionStation.getDirection().getId(), key -> new ArrayList<>()).add(directionStation);
        }
        return map;
    }

    private Map<Integer, List<RoutePoint>> groupRoutePoints(Collection<Integer> directionIds) {
        Map<Integer, List<RoutePoint>> map = new HashMap<>();
        if (directionIds.isEmpty()) {
            return map;
        }
        List<RoutePoint> all = routePointRepository.findByDirection_IdInOrderByDirection_IdAscSequenceOrderAsc(directionIds);
        for (RoutePoint routePoint : all) {
            map.computeIfAbsent(routePoint.getDirection().getId(), key -> new ArrayList<>()).add(routePoint);
        }
        return map;
    }

    private int toGtfsRouteType(Short vehicleTypeId) {
        if (vehicleTypeId == null) {
            return 3;
        }
        return switch (vehicleTypeId) {
            case 4 -> 0;
            case 3 -> 11;
            default -> 3;
        };
    }

    private int toDirectionId(Direction direction) {
        String label = direction.getDirectionLabel();
        if (label == null || label.isBlank()) {
            return 0;
        }
        String normalized = label.toLowerCase(Locale.ROOT);
        return normalized.contains("return") || normalized.contains("back") ? 1 : 0;
    }

    private String serviceKey(Timetable timetable) {
        Set<Short> set = daySet(timetable.getDaysOfWeek());
        LocalDate start = timetable.getValidFrom() == null ? LocalDate.now().minusDays(1) : timetable.getValidFrom();
        LocalDate end = timetable.getValidTo() == null ? LocalDate.now().plusYears(1) : timetable.getValidTo();
        return set.contains((short) 1) + "|"
                + set.contains((short) 2) + "|"
                + set.contains((short) 3) + "|"
                + set.contains((short) 4) + "|"
                + set.contains((short) 5) + "|"
                + set.contains((short) 6) + "|"
                + set.contains((short) 7) + "|"
                + GTFS_DATE.format(start) + "|"
                + GTFS_DATE.format(end);
    }

    private String[] calendarRow(String serviceId, Timetable timetable) {
        Set<Short> set = daySet(timetable.getDaysOfWeek());
        LocalDate start = timetable.getValidFrom() == null ? LocalDate.now().minusDays(1) : timetable.getValidFrom();
        LocalDate end = timetable.getValidTo() == null ? LocalDate.now().plusYears(1) : timetable.getValidTo();

        return new String[]{
                serviceId,
                bool01(set.contains((short) 1)),
                bool01(set.contains((short) 2)),
                bool01(set.contains((short) 3)),
                bool01(set.contains((short) 4)),
                bool01(set.contains((short) 5)),
                bool01(set.contains((short) 6)),
                bool01(set.contains((short) 7)),
                GTFS_DATE.format(start),
                GTFS_DATE.format(end)
        };
    }

    private String bool01(boolean value) {
        return value ? "1" : "0";
    }

    private Set<Short> daySet(Short[] days) {
        Set<Short> set = new HashSet<>();
        if (days == null) {
            return set;
        }
        for (Short day : days) {
            if (day != null) {
                set.add(day);
            }
        }
        return set;
    }

    private String formatGtfsTime(LocalTime time) {
        return GTFS_TIME.format(time);
    }

    private String routeId(Integer lineId) {
        return "line-" + lineId;
    }

    private String tripId(Integer timetableId) {
        return "timetable-" + timetableId;
    }

    private String shapeId(Integer directionId) {
        return "direction-" + directionId;
    }

    private String stopId(Integer stationId) {
        return "stop-" + stationId;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String toCsv(List<String[]> rows) {
        StringBuilder builder = new StringBuilder();
        for (String[] row : rows) {
            for (int i = 0; i < row.length; i++) {
                if (i > 0) {
                    builder.append(',');
                }
                builder.append(escapeCsv(row[i]));
            }
            builder.append('\n');
        }
        return builder.toString();
    }

    private String escapeCsv(String value) {
        String safe = value == null ? "" : value;
        boolean quote = safe.contains(",") || safe.contains("\"") || safe.contains("\n") || safe.contains("\r");
        if (!quote) {
            return safe;
        }
        return '"' + safe.replace("\"", "\"\"") + '"';
    }

    private byte[] zipFiles(Map<String, String> files) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (ZipOutputStream zipOutputStream = new ZipOutputStream(baos, StandardCharsets.UTF_8)) {
                for (Map.Entry<String, String> file : files.entrySet()) {
                    ZipEntry entry = new ZipEntry(file.getKey());
                    zipOutputStream.putNextEntry(entry);
                    zipOutputStream.write(file.getValue().getBytes(StandardCharsets.UTF_8));
                    zipOutputStream.closeEntry();
                }
            }
            return baos.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to build GTFS zip", ex);
        }
    }

    private record CalendarBuild(List<String[]> rows, Map<Integer, String> serviceIdByTimetableId) {
    }

    private record StopTimesBuild(List<String[]> rows, int emittedCount) {
    }
}
