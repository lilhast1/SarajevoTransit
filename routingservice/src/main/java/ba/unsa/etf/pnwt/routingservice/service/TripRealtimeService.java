package ba.unsa.etf.pnwt.routingservice.service;

import ba.unsa.etf.pnwt.routingservice.dto.TripDelayResponse;
import ba.unsa.etf.pnwt.routingservice.dto.TripDelayUpsertRequest;
import ba.unsa.etf.pnwt.routingservice.event.TripDelayChangedEvent;
import ba.unsa.etf.pnwt.routingservice.exception.ResourceNotFoundException;
import ba.unsa.etf.pnwt.routingservice.messaging.TimetableEventPublisher;
import ba.unsa.etf.pnwt.routingservice.model.Line;
import ba.unsa.etf.pnwt.routingservice.model.Timetable;
import ba.unsa.etf.pnwt.routingservice.model.DirectionStation;
import ba.unsa.etf.pnwt.routingservice.realtime.TripDelayEntry;
import ba.unsa.etf.pnwt.routingservice.repository.DirectionStationRepository;
import ba.unsa.etf.pnwt.routingservice.repository.TimetableRepository;
import com.google.transit.realtime.GtfsRealtime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TripRealtimeService {

    private static final DateTimeFormatter SERVICE_DATE = DateTimeFormatter.BASIC_ISO_DATE;

    private final TimetableRepository timetableRepository;
    private final DirectionStationRepository directionStationRepository;
    private final TimetableEventPublisher timetableEventPublisher;
    private final Map<String, TripDelayEntry> delays = new ConcurrentHashMap<>();

    public TripRealtimeService(
            TimetableRepository timetableRepository,
            DirectionStationRepository directionStationRepository,
            TimetableEventPublisher timetableEventPublisher
    ) {
        this.timetableRepository = timetableRepository;
        this.directionStationRepository = directionStationRepository;
        this.timetableEventPublisher = timetableEventPublisher;
    }

    @Transactional
    public TripDelayResponse setDelay(TripDelayUpsertRequest request) {
        validateServiceDate(request.getServiceDate());
        Timetable timetable = findTimetable(request.getTimetableId());
        Line line = timetable.getLine();

        Instant now = Instant.now();
        TripDelayEntry entry = new TripDelayEntry(
                timetable.getId(),
                timetable.getDirection().getId(),
                request.getServiceDate(),
                request.getDelaySeconds(),
                request.getReason(),
                line.getId().longValue(),
                line.getCode(),
                line.getName(),
                now
        );
        delays.put(key(timetable.getId(), request.getServiceDate()), entry);

        timetableEventPublisher.publishTripDelayChanged(new TripDelayChangedEvent(
                UUID.randomUUID().toString(),
                "SET",
                timetable.getId(),
                request.getServiceDate(),
                request.getDelaySeconds(),
                request.getReason(),
                line.getId().longValue(),
                line.getCode(),
                line.getName(),
                now
        ));

        return toResponse(entry);
    }

    @Transactional
    public void clearDelay(Integer timetableId, String serviceDate) {
        validateServiceDate(serviceDate);
        Timetable timetable = findTimetable(timetableId);
        Line line = timetable.getLine();
        delays.remove(key(timetableId, serviceDate));

        timetableEventPublisher.publishTripDelayChanged(new TripDelayChangedEvent(
                UUID.randomUUID().toString(),
                "CLEAR",
                timetableId,
                serviceDate,
                0,
                null,
                line.getId().longValue(),
                line.getCode(),
                line.getName(),
                Instant.now()
        ));
    }

    public List<TripDelayResponse> getDelays() {
        return delays.values().stream()
                .sorted(Comparator.comparing(TripDelayEntry::updatedAt).reversed())
                .map(this::toResponse)
                .toList();
    }

    public byte[] buildTripUpdatesFeed() {
        GtfsRealtime.FeedHeader header = GtfsRealtime.FeedHeader.newBuilder()
                .setGtfsRealtimeVersion("2.0")
                .setTimestamp(Instant.now().getEpochSecond())
                .build();

        GtfsRealtime.FeedMessage.Builder feedBuilder = GtfsRealtime.FeedMessage.newBuilder().setHeader(header);

        for (TripDelayEntry entry : delays.values()) {
            List<DirectionStation> stops = directionStationRepository.findByDirection_IdOrderByStopSequenceAsc(entry.directionId());
            if (stops.isEmpty()) {
                continue;
            }

            GtfsRealtime.TripDescriptor tripDescriptor = GtfsRealtime.TripDescriptor.newBuilder()
                    .setTripId(tripId(entry.timetableId()))
                    .setStartDate(entry.serviceDate())
                    .build();

            GtfsRealtime.TripUpdate.Builder tripUpdateBuilder = GtfsRealtime.TripUpdate.newBuilder()
                    .setTrip(tripDescriptor)
                    .setDelay(entry.delaySeconds());

            for (DirectionStation stop : stops) {
                if (stop.getStopSequence() == null) {
                    continue;
                }
                GtfsRealtime.TripUpdate.StopTimeEvent delayEvent = GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder()
                        .setDelay(entry.delaySeconds())
                        .build();
                GtfsRealtime.TripUpdate.StopTimeUpdate stopUpdate = GtfsRealtime.TripUpdate.StopTimeUpdate.newBuilder()
                        .setStopSequence(stop.getStopSequence())
                        .setArrival(delayEvent)
                        .setDeparture(delayEvent)
                        .build();
                tripUpdateBuilder.addStopTimeUpdate(stopUpdate);
            }

            GtfsRealtime.TripUpdate tripUpdate = tripUpdateBuilder.build();

            GtfsRealtime.FeedEntity entity = GtfsRealtime.FeedEntity.newBuilder()
                    .setId(tripId(entry.timetableId()) + "-" + entry.serviceDate())
                    .setTripUpdate(tripUpdate)
                    .build();

            feedBuilder.addEntity(entity);
        }

        return feedBuilder.build().toByteArray();
    }

    private Timetable findTimetable(Integer id) {
        return timetableRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Timetable not found: id=" + id));
    }

    private TripDelayResponse toResponse(TripDelayEntry entry) {
        TripDelayResponse response = new TripDelayResponse();
        response.setTripId(tripId(entry.timetableId()));
        response.setTimetableId(entry.timetableId());
        response.setServiceDate(entry.serviceDate());
        response.setDelaySeconds(entry.delaySeconds());
        response.setReason(entry.reason());
        response.setLineId(entry.lineId());
        response.setLineCode(entry.lineCode());
        response.setLineName(entry.lineName());
        response.setUpdatedAt(entry.updatedAt());
        return response;
    }

    private String key(Integer timetableId, String serviceDate) {
        return timetableId + "|" + serviceDate;
    }

    private String tripId(Integer timetableId) {
        return "timetable-" + timetableId;
    }

    private void validateServiceDate(String value) {
        try {
            LocalDate.parse(value, SERVICE_DATE);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("serviceDate must be in yyyyMMdd format");
        }
    }
}
