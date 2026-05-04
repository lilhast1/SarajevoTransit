package ba.unsa.etf.pnwt.routingservice.service;

import ba.unsa.etf.pnwt.routingservice.dto.DirectionRequest;
import ba.unsa.etf.pnwt.routingservice.dto.DirectionResponse;
import ba.unsa.etf.pnwt.routingservice.dto.DirectionStationRequest;
import ba.unsa.etf.pnwt.routingservice.dto.DirectionStationResponse;
import ba.unsa.etf.pnwt.routingservice.dto.LineRequest;
import ba.unsa.etf.pnwt.routingservice.dto.LineResponse;
import ba.unsa.etf.pnwt.routingservice.dto.RoutePointRequest;
import ba.unsa.etf.pnwt.routingservice.dto.RoutePointResponse;
import ba.unsa.etf.pnwt.routingservice.dto.StationRequest;
import ba.unsa.etf.pnwt.routingservice.dto.StationResponse;
import ba.unsa.etf.pnwt.routingservice.dto.TimetableRequest;
import ba.unsa.etf.pnwt.routingservice.dto.TimetableResponse;
import ba.unsa.etf.pnwt.routingservice.exception.ConflictException;
import ba.unsa.etf.pnwt.routingservice.exception.ResourceNotFoundException;
import ba.unsa.etf.pnwt.routingservice.mapper.RoutingMapper;
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
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Transactional
public class RoutingCrudService {

    private final LineRepository lineRepository;
    private final DirectionRepository directionRepository;
    private final StationRepository stationRepository;
    private final TimetableRepository timetableRepository;
    private final DirectionStationRepository directionStationRepository;
    private final RoutePointRepository routePointRepository;
    private final VehicleTypeRepository vehicleTypeRepository;
    private final RoutingMapper mapper;

    public RoutingCrudService(
            LineRepository lineRepository,
            DirectionRepository directionRepository,
            StationRepository stationRepository,
            TimetableRepository timetableRepository,
            DirectionStationRepository directionStationRepository,
            RoutePointRepository routePointRepository,
            VehicleTypeRepository vehicleTypeRepository,
            RoutingMapper mapper
    ) {
        this.lineRepository = lineRepository;
        this.directionRepository = directionRepository;
        this.stationRepository = stationRepository;
        this.timetableRepository = timetableRepository;
        this.directionStationRepository = directionStationRepository;
        this.routePointRepository = routePointRepository;
        this.vehicleTypeRepository = vehicleTypeRepository;
        this.mapper = mapper;
    }

    public List<LineResponse> getLines(Boolean activeOnly, Short vehicleTypeId) {
        List<Line> lines;
        if (vehicleTypeId != null && Boolean.TRUE.equals(activeOnly)) {
            lines = lineRepository.findByVehicleType_IdAndIsActiveTrue(vehicleTypeId);
        } else if (vehicleTypeId != null) {
            lines = lineRepository.findByVehicleType_Id(vehicleTypeId);
        } else if (Boolean.TRUE.equals(activeOnly)) {
            lines = lineRepository.findByIsActiveTrue();
        } else {
            lines = lineRepository.findAll();
        }
        return lines.stream().map(mapper::toLineResponse).toList();
    }

    public LineResponse getLine(Integer id) {
        return mapper.toLineResponse(findLine(id));
    }

    public LineResponse createLine(LineRequest request) {
        validateLineExternalIdOnCreate(request.getExternalId());

        VehicleType vehicleType = findVehicleType(request.getVehicleTypeId());
        Line line = new Line();
        applyLineRequest(line, request, vehicleType);
        return mapper.toLineResponse(lineRepository.save(line));
    }

    public LineResponse updateLine(Integer id, LineRequest request) {
        Line line = findLine(id);
        validateLineExternalIdOnUpdate(id, request.getExternalId());
        VehicleType vehicleType = findVehicleType(request.getVehicleTypeId());
        applyLineRequest(line, request, vehicleType);
        return mapper.toLineResponse(lineRepository.save(line));
    }

    public void deleteLine(Integer id) {
        Line line = findLine(id);
        lineRepository.delete(line);
    }

    public List<DirectionResponse> getDirections(Boolean activeOnly, Integer lineId) {
        List<Direction> directions;
        if (lineId != null && Boolean.TRUE.equals(activeOnly)) {
            directions = directionRepository.findByLine_IdAndIsActiveTrue(lineId);
        } else if (lineId != null) {
            directions = directionRepository.findByLine_Id(lineId);
        } else if (Boolean.TRUE.equals(activeOnly)) {
            directions = directionRepository.findByIsActiveTrue();
        } else {
            directions = directionRepository.findAll();
        }
        return directions.stream().map(mapper::toDirectionResponse).toList();
    }

    public DirectionResponse getDirection(Integer id) {
        return mapper.toDirectionResponse(findDirection(id));
    }

    public DirectionResponse createDirection(DirectionRequest request) {
        validateDirectionExternalIdOnCreate(request.getExternalId());
        Line line = findLine(request.getLineId());

        Direction direction = new Direction();
        applyDirectionRequest(direction, request, line);
        return mapper.toDirectionResponse(directionRepository.save(direction));
    }

    public DirectionResponse updateDirection(Integer id, DirectionRequest request) {
        Direction direction = findDirection(id);
        validateDirectionExternalIdOnUpdate(id, request.getExternalId());
        Line line = findLine(request.getLineId());
        applyDirectionRequest(direction, request, line);
        return mapper.toDirectionResponse(directionRepository.save(direction));
    }

    public void deleteDirection(Integer id) {
        Direction direction = findDirection(id);
        directionRepository.delete(direction);
    }

    public List<StationResponse> getStations(Boolean activeOnly, String name) {
        List<Station> stations;
        if (name != null && !name.isBlank() && Boolean.TRUE.equals(activeOnly)) {
            stations = stationRepository.findByNameContainingIgnoreCaseAndIsActive(name, true);
        } else if (name != null && !name.isBlank()) {
            stations = stationRepository.findByNameContainingIgnoreCase(name);
        } else if (activeOnly != null) {
            stations = stationRepository.findByIsActive(activeOnly);
        } else {
            stations = stationRepository.findAll();
        }
        return stations.stream().map(mapper::toStationResponse).toList();
    }

    public StationResponse getStation(Integer id) {
        return mapper.toStationResponse(findStation(id));
    }

    public StationResponse createStation(StationRequest request) {
        validateStationExternalIdOnCreate(request.getExternalId());
        Station station = new Station();
        applyStationRequest(station, request);
        return mapper.toStationResponse(stationRepository.save(station));
    }

    public StationResponse updateStation(Integer id, StationRequest request) {
        Station station = findStation(id);
        validateStationExternalIdOnUpdate(id, request.getExternalId());
        applyStationRequest(station, request);
        return mapper.toStationResponse(stationRepository.save(station));
    }

    public void deleteStation(Integer id) {
        Station station = findStation(id);
        stationRepository.delete(station);
    }

    public List<TimetableResponse> getTimetables(Integer lineId, Integer directionId, Boolean activeOnly) {
        List<Timetable> timetables;
        if (directionId != null && Boolean.TRUE.equals(activeOnly)) {
            timetables = timetableRepository.findByDirection_IdAndIsActiveTrueOrderByDepartureTimeAsc(directionId);
        } else if (lineId != null && Boolean.TRUE.equals(activeOnly)) {
            timetables = timetableRepository.findByLine_IdAndIsActiveTrueOrderByDepartureTimeAsc(lineId);
        } else if (Boolean.TRUE.equals(activeOnly)) {
            timetables = timetableRepository.findByIsActiveTrueOrderByDepartureTimeAsc();
        } else {
            timetables = timetableRepository.findAll();
        }
        return timetables.stream().map(mapper::toTimetableResponse).toList();
    }

    public TimetableResponse getTimetable(Integer id) {
        return mapper.toTimetableResponse(findTimetable(id));
    }

    public TimetableResponse createTimetable(TimetableRequest request) {
        validateTimetableExternalIdOnCreate(request.getExternalId());
        Direction direction = findDirection(request.getDirectionId());
        Line line = findLine(request.getLineId());
        validateTimetableLineDirectionConsistency(direction, line);

        Timetable timetable = new Timetable();
        applyTimetableRequest(timetable, request, direction, line);
        return mapper.toTimetableResponse(timetableRepository.save(timetable));
    }

    public TimetableResponse updateTimetable(Integer id, TimetableRequest request) {
        Timetable timetable = findTimetable(id);
        validateTimetableExternalIdOnUpdate(id, request.getExternalId());
        Direction direction = findDirection(request.getDirectionId());
        Line line = findLine(request.getLineId());
        validateTimetableLineDirectionConsistency(direction, line);
        applyTimetableRequest(timetable, request, direction, line);
        return mapper.toTimetableResponse(timetableRepository.save(timetable));
    }

    public void deleteTimetable(Integer id) {
        Timetable timetable = findTimetable(id);
        timetableRepository.delete(timetable);
    }

    public List<DirectionStationResponse> getDirectionStationsByDirection(Integer directionId) {
        return directionStationRepository.findByDirection_IdOrderByStopSequenceAsc(directionId)
                .stream()
                .map(mapper::toDirectionStationResponse)
                .toList();
    }

    public DirectionStationResponse createDirectionStation(DirectionStationRequest request) {
        Direction direction = findDirection(request.getDirectionId());
        Station station = findStation(request.getStationId());

        ensureDirectionStationUnique(direction.getId(), station.getId(), request.getStopSequence(), null);

        DirectionStation directionStation = new DirectionStation();
        directionStation.setDirection(direction);
        directionStation.setStation(station);
        directionStation.setStopSequence(request.getStopSequence());
        directionStation.setTravelTimeFromPrevSeconds(request.getTravelTimeFromPrevSeconds());

        return mapper.toDirectionStationResponse(directionStationRepository.save(directionStation));
    }

    public DirectionStationResponse updateDirectionStation(Integer id, DirectionStationRequest request) {
        DirectionStation directionStation = directionStationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("DirectionStation not found: id=" + id));
        Direction direction = findDirection(request.getDirectionId());
        Station station = findStation(request.getStationId());

        ensureDirectionStationUnique(direction.getId(), station.getId(), request.getStopSequence(), id);

        directionStation.setDirection(direction);
        directionStation.setStation(station);
        directionStation.setStopSequence(request.getStopSequence());
        directionStation.setTravelTimeFromPrevSeconds(request.getTravelTimeFromPrevSeconds());

        return mapper.toDirectionStationResponse(directionStationRepository.save(directionStation));
    }

    public void deleteDirectionStation(Integer id) {
        DirectionStation directionStation = directionStationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("DirectionStation not found: id=" + id));
        directionStationRepository.delete(directionStation);
    }

    public List<RoutePointResponse> getRoutePointsByDirection(Integer directionId) {
        return routePointRepository.findByDirection_IdOrderBySequenceOrderAsc(directionId)
                .stream()
                .map(mapper::toRoutePointResponse)
                .toList();
    }

    public RoutePointResponse createRoutePoint(RoutePointRequest request) {
        Direction direction = findDirection(request.getDirectionId());
        ensureRoutePointSequenceUnique(direction.getId(), request.getSequenceOrder(), null);

        RoutePoint routePoint = new RoutePoint();
        routePoint.setDirection(direction);
        routePoint.setSegmentId(request.getSegmentId());
        routePoint.setSequenceOrder(request.getSequenceOrder());
        routePoint.setLatitude(request.getLatitude());
        routePoint.setLongitude(request.getLongitude());

        return mapper.toRoutePointResponse(routePointRepository.save(routePoint));
    }

    public RoutePointResponse updateRoutePoint(Integer id, RoutePointRequest request) {
        RoutePoint routePoint = routePointRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("RoutePoint not found: id=" + id));
        Direction direction = findDirection(request.getDirectionId());
        ensureRoutePointSequenceUnique(direction.getId(), request.getSequenceOrder(), id);

        routePoint.setDirection(direction);
        routePoint.setSegmentId(request.getSegmentId());
        routePoint.setSequenceOrder(request.getSequenceOrder());
        routePoint.setLatitude(request.getLatitude());
        routePoint.setLongitude(request.getLongitude());

        return mapper.toRoutePointResponse(routePointRepository.save(routePoint));
    }

    public void deleteRoutePoint(Integer id) {
        RoutePoint routePoint = routePointRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("RoutePoint not found: id=" + id));
        routePointRepository.delete(routePoint);
    }

    private void applyLineRequest(Line line, LineRequest request, VehicleType vehicleType) {
        line.setExternalId(request.getExternalId());
        line.setCode(request.getCode());
        line.setName(request.getName());
        line.setVehicleType(vehicleType);
        line.setIsActive(request.getIsActive());
    }

    private void applyDirectionRequest(Direction direction, DirectionRequest request, Line line) {
        direction.setExternalId(request.getExternalId());
        direction.setLine(line);
        direction.setCode(request.getCode());
        direction.setName(request.getName());
        direction.setDirectionLabel(request.getDirectionLabel());
        direction.setLengthMeters(request.getLengthMeters() == null ? 0D : request.getLengthMeters());
        direction.setCanDelete(request.getCanDelete());
        direction.setIsActive(request.getIsActive());
    }

    private void applyStationRequest(Station station, StationRequest request) {
        station.setExternalId(request.getExternalId());
        station.setCode(request.getCode());
        station.setName(request.getName());
        station.setAddress(request.getAddress());
        station.setLatitude(request.getLatitude());
        station.setLongitude(request.getLongitude());
        station.setIsActive(request.getIsActive());
    }

    private void applyTimetableRequest(Timetable timetable, TimetableRequest request, Direction direction, Line line) {
        timetable.setExternalId(request.getExternalId());
        timetable.setDirection(direction);
        timetable.setLine(line);
        timetable.setName(request.getName());
        timetable.setDepartureTime(request.getDepartureTime());
        timetable.setValidFrom(request.getValidFrom());
        timetable.setValidTo(request.getValidTo());
        timetable.setRidesOnHolidays(request.getRidesOnHolidays());
        timetable.setDaysOfWeek(request.getDaysOfWeek().toArray(new Short[0]));
        timetable.setReceivesPassengers(request.getReceivesPassengers());
        timetable.setIsActive(request.getIsActive());
    }

    private Line findLine(Integer id) {
        return lineRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Line not found: id=" + id));
    }

    private Direction findDirection(Integer id) {
        return directionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Direction not found: id=" + id));
    }

    private Station findStation(Integer id) {
        return stationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Station not found: id=" + id));
    }

    private Timetable findTimetable(Integer id) {
        return timetableRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Timetable not found: id=" + id));
    }

    private VehicleType findVehicleType(Short id) {
        return vehicleTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("VehicleType not found: id=" + id));
    }

    private void validateLineExternalIdOnCreate(Integer externalId) {
        if (externalId != null && lineRepository.existsByExternalId(externalId)) {
            throw new ConflictException("Line with externalId=" + externalId + " already exists");
        }
    }

    private void validateLineExternalIdOnUpdate(Integer id, Integer externalId) {
        if (externalId != null && lineRepository.existsByExternalIdAndIdNot(externalId, id)) {
            throw new ConflictException("Another line with externalId=" + externalId + " already exists");
        }
    }

    private void validateDirectionExternalIdOnCreate(Integer externalId) {
        if (externalId != null && directionRepository.existsByExternalId(externalId)) {
            throw new ConflictException("Direction with externalId=" + externalId + " already exists");
        }
    }

    private void validateDirectionExternalIdOnUpdate(Integer id, Integer externalId) {
        if (externalId != null && directionRepository.existsByExternalIdAndIdNot(externalId, id)) {
            throw new ConflictException("Another direction with externalId=" + externalId + " already exists");
        }
    }

    private void validateStationExternalIdOnCreate(Integer externalId) {
        if (externalId != null && stationRepository.existsByExternalId(externalId)) {
            throw new ConflictException("Station with externalId=" + externalId + " already exists");
        }
    }

    private void validateStationExternalIdOnUpdate(Integer id, Integer externalId) {
        if (externalId != null && stationRepository.existsByExternalIdAndIdNot(externalId, id)) {
            throw new ConflictException("Another station with externalId=" + externalId + " already exists");
        }
    }

    private void validateTimetableExternalIdOnCreate(Integer externalId) {
        if (externalId != null && timetableRepository.existsByExternalId(externalId)) {
            throw new ConflictException("Timetable with externalId=" + externalId + " already exists");
        }
    }

    private void validateTimetableExternalIdOnUpdate(Integer id, Integer externalId) {
        if (externalId != null && timetableRepository.existsByExternalIdAndIdNot(externalId, id)) {
            throw new ConflictException("Another timetable with externalId=" + externalId + " already exists");
        }
    }

    private void validateTimetableLineDirectionConsistency(Direction direction, Line line) {
        if (!direction.getLine().getId().equals(line.getId())) {
            throw new ConflictException("Direction id=" + direction.getId() + " does not belong to line id=" + line.getId());
        }
    }

    private void ensureDirectionStationUnique(Integer directionId, Integer stationId, Integer stopSequence, Integer excludeId) {
        List<DirectionStation> existing = directionStationRepository.findByDirection_IdOrderByStopSequenceAsc(directionId);
        for (DirectionStation item : existing) {
            if (excludeId != null && excludeId.equals(item.getId())) {
                continue;
            }
            if (item.getStopSequence().equals(stopSequence)) {
                throw new ConflictException("DirectionStation with directionId=" + directionId + " and stopSequence=" + stopSequence + " already exists");
            }
            if (item.getStation().getId().equals(stationId)) {
                throw new ConflictException("DirectionStation with directionId=" + directionId + " and stationId=" + stationId + " already exists");
            }
        }
    }

    private void ensureRoutePointSequenceUnique(Integer directionId, Integer sequenceOrder, Integer excludeId) {
        List<RoutePoint> existing = routePointRepository.findByDirection_IdOrderBySequenceOrderAsc(directionId);
        for (RoutePoint item : existing) {
            if (excludeId != null && excludeId.equals(item.getId())) {
                continue;
            }
            if (item.getSequenceOrder().equals(sequenceOrder)) {
                throw new ConflictException("RoutePoint with directionId=" + directionId + " and sequenceOrder=" + sequenceOrder + " already exists");
            }
        }
    }
}
