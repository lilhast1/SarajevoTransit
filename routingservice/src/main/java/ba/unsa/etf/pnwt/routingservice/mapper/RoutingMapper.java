package ba.unsa.etf.pnwt.routingservice.mapper;

import ba.unsa.etf.pnwt.routingservice.dto.DirectionResponse;
import ba.unsa.etf.pnwt.routingservice.dto.DirectionStationResponse;
import ba.unsa.etf.pnwt.routingservice.dto.LineResponse;
import ba.unsa.etf.pnwt.routingservice.dto.RoutePointResponse;
import ba.unsa.etf.pnwt.routingservice.dto.StationResponse;
import ba.unsa.etf.pnwt.routingservice.dto.TimetableResponse;
import ba.unsa.etf.pnwt.routingservice.model.Direction;
import ba.unsa.etf.pnwt.routingservice.model.DirectionStation;
import ba.unsa.etf.pnwt.routingservice.model.Line;
import ba.unsa.etf.pnwt.routingservice.model.RoutePoint;
import ba.unsa.etf.pnwt.routingservice.model.Station;
import ba.unsa.etf.pnwt.routingservice.model.Timetable;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;

@Component
public class RoutingMapper {

    public LineResponse toLineResponse(Line line) {
        LineResponse response = new LineResponse();
        response.setId(line.getId());
        response.setExternalId(line.getExternalId());
        response.setCode(line.getCode());
        response.setName(line.getName());
        response.setVehicleTypeId(line.getVehicleType().getId());
        response.setVehicleTypeName(line.getVehicleType().getName());
        response.setIsActive(line.getIsActive());
        response.setCreatedAt(line.getCreatedAt());
        response.setUpdatedAt(line.getUpdatedAt());
        return response;
    }

    public DirectionResponse toDirectionResponse(Direction direction) {
        DirectionResponse response = new DirectionResponse();
        response.setId(direction.getId());
        response.setExternalId(direction.getExternalId());
        response.setLineId(direction.getLine().getId());
        response.setLineExternalId(direction.getLine().getExternalId());
        response.setLineName(direction.getLine().getName());
        response.setCode(direction.getCode());
        response.setName(direction.getName());
        response.setDirectionLabel(direction.getDirectionLabel());
        response.setLengthMeters(direction.getLengthMeters());
        response.setCanDelete(direction.getCanDelete());
        response.setIsActive(direction.getIsActive());
        response.setCreatedAt(direction.getCreatedAt());
        response.setUpdatedAt(direction.getUpdatedAt());
        return response;
    }

    public StationResponse toStationResponse(Station station) {
        StationResponse response = new StationResponse();
        response.setId(station.getId());
        response.setExternalId(station.getExternalId());
        response.setCode(station.getCode());
        response.setName(station.getName());
        response.setAddress(station.getAddress());
        response.setLatitude(station.getLatitude());
        response.setLongitude(station.getLongitude());
        response.setIsActive(station.getIsActive());
        response.setCreatedAt(station.getCreatedAt());
        response.setUpdatedAt(station.getUpdatedAt());
        return response;
    }

    public TimetableResponse toTimetableResponse(Timetable timetable) {
        TimetableResponse response = new TimetableResponse();
        response.setId(timetable.getId());
        response.setExternalId(timetable.getExternalId());
        response.setDirectionId(timetable.getDirection().getId());
        response.setDirectionExternalId(timetable.getDirection().getExternalId());
        response.setDirectionName(timetable.getDirection().getName());
        response.setLineId(timetable.getLine().getId());
        response.setLineExternalId(timetable.getLine().getExternalId());
        response.setLineName(timetable.getLine().getName());
        response.setName(timetable.getName());
        response.setDepartureTime(timetable.getDepartureTime());
        response.setValidFrom(timetable.getValidFrom());
        response.setValidTo(timetable.getValidTo());
        response.setRidesOnHolidays(timetable.getRidesOnHolidays());
        response.setDaysOfWeek(timetable.getDaysOfWeek() == null
                ? Collections.emptyList()
                : Arrays.asList(timetable.getDaysOfWeek()));
        response.setReceivesPassengers(timetable.getReceivesPassengers());
        response.setIsActive(timetable.getIsActive());
        response.setCreatedAt(timetable.getCreatedAt());
        response.setUpdatedAt(timetable.getUpdatedAt());
        return response;
    }

    public DirectionStationResponse toDirectionStationResponse(DirectionStation directionStation) {
        DirectionStationResponse response = new DirectionStationResponse();
        response.setId(directionStation.getId());
        response.setDirectionId(directionStation.getDirection().getId());
        response.setDirectionExternalId(directionStation.getDirection().getExternalId());
        response.setDirectionName(directionStation.getDirection().getName());
        response.setStationId(directionStation.getStation().getId());
        response.setStationExternalId(directionStation.getStation().getExternalId());
        response.setStationName(directionStation.getStation().getName());
        response.setStopSequence(directionStation.getStopSequence());
        response.setTravelTimeFromPrevSeconds(directionStation.getTravelTimeFromPrevSeconds());
        return response;
    }

    public RoutePointResponse toRoutePointResponse(RoutePoint routePoint) {
        RoutePointResponse response = new RoutePointResponse();
        response.setId(routePoint.getId());
        response.setDirectionId(routePoint.getDirection().getId());
        response.setDirectionExternalId(routePoint.getDirection().getExternalId());
        response.setDirectionName(routePoint.getDirection().getName());
        response.setSegmentId(routePoint.getSegmentId());
        response.setSequenceOrder(routePoint.getSequenceOrder());
        response.setLatitude(routePoint.getLatitude());
        response.setLongitude(routePoint.getLongitude());
        return response;
    }
}
