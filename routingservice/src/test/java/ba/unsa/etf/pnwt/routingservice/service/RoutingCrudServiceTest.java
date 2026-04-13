package ba.unsa.etf.pnwt.routingservice.service;

import ba.unsa.etf.pnwt.routingservice.dto.LineRequest;
import ba.unsa.etf.pnwt.routingservice.dto.LineResponse;
import ba.unsa.etf.pnwt.routingservice.dto.TimetableRequest;
import ba.unsa.etf.pnwt.routingservice.exception.ConflictException;
import ba.unsa.etf.pnwt.routingservice.mapper.RoutingMapper;
import ba.unsa.etf.pnwt.routingservice.model.Direction;
import ba.unsa.etf.pnwt.routingservice.model.Line;
import ba.unsa.etf.pnwt.routingservice.model.VehicleType;
import ba.unsa.etf.pnwt.routingservice.repository.DirectionRepository;
import ba.unsa.etf.pnwt.routingservice.repository.DirectionStationRepository;
import ba.unsa.etf.pnwt.routingservice.repository.LineRepository;
import ba.unsa.etf.pnwt.routingservice.repository.RoutePointRepository;
import ba.unsa.etf.pnwt.routingservice.repository.StationRepository;
import ba.unsa.etf.pnwt.routingservice.repository.TimetableRepository;
import ba.unsa.etf.pnwt.routingservice.repository.VehicleTypeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoutingCrudServiceTest {

    @Mock private LineRepository lineRepository;
    @Mock private DirectionRepository directionRepository;
    @Mock private StationRepository stationRepository;
    @Mock private TimetableRepository timetableRepository;
    @Mock private DirectionStationRepository directionStationRepository;
    @Mock private RoutePointRepository routePointRepository;
    @Mock private VehicleTypeRepository vehicleTypeRepository;
    @Mock private RoutingMapper mapper;

    private RoutingCrudService service;

    @BeforeEach
    void setUp() {
        service = new RoutingCrudService(
                lineRepository,
                directionRepository,
                stationRepository,
                timetableRepository,
                directionStationRepository,
                routePointRepository,
                vehicleTypeRepository,
                mapper
        );
    }

    @Test
    void createLineHappyPath() {
        LineRequest request = new LineRequest();
        request.setExternalId(1234);
        request.setCode("10");
        request.setName("Test line");
        request.setVehicleTypeId((short) 2);
        request.setIsActive(true);

        VehicleType vehicleType = new VehicleType();
        vehicleType.setId((short) 2);
        vehicleType.setName("bus");

        Line saved = new Line();
        saved.setId(1);
        saved.setExternalId(1234);
        saved.setVehicleType(vehicleType);
        saved.setName("Test line");

        LineResponse mapped = new LineResponse();
        mapped.setId(1);
        mapped.setName("Test line");

        when(lineRepository.existsByExternalId(1234)).thenReturn(false);
        when(vehicleTypeRepository.findById((short) 2)).thenReturn(Optional.of(vehicleType));
        when(lineRepository.save(any(Line.class))).thenReturn(saved);
        when(mapper.toLineResponse(saved)).thenReturn(mapped);

        LineResponse response = service.createLine(request);

        assertEquals(1, response.getId());
        assertEquals("Test line", response.getName());
    }

    @Test
    void createLineWithDuplicateExternalIdThrowsConflict() {
        LineRequest request = new LineRequest();
        request.setExternalId(999);
        request.setVehicleTypeId((short) 2);
        request.setName("Duplicate");
        request.setIsActive(true);

        when(lineRepository.existsByExternalId(999)).thenReturn(true);

        assertThrows(ConflictException.class, () -> service.createLine(request));
        verify(lineRepository, never()).save(any(Line.class));
    }

    @Test
    void createTimetableWithLineDirectionMismatchThrowsConflict() {
        Line lineA = new Line();
        lineA.setId(1);

        Line lineB = new Line();
        lineB.setId(2);

        Direction direction = new Direction();
        direction.setId(10);
        direction.setLine(lineA);

        TimetableRequest request = new TimetableRequest();
        request.setDirectionId(10);
        request.setLineId(2);
        request.setDepartureTime(LocalTime.of(12, 0));
        request.setDaysOfWeek(List.of((short) 1));
        request.setRidesOnHolidays(false);
        request.setReceivesPassengers(true);
        request.setIsActive(true);

        when(directionRepository.findById(10)).thenReturn(Optional.of(direction));
        when(lineRepository.findById(2)).thenReturn(Optional.of(lineB));

        assertThrows(ConflictException.class, () -> service.createTimetable(request));
    }
}
