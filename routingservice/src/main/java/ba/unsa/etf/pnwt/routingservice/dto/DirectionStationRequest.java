package ba.unsa.etf.pnwt.routingservice.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class DirectionStationRequest {

    @NotNull
    private Integer directionId;

    @NotNull
    private Integer stationId;

    @NotNull
    @Min(1)
    private Integer stopSequence;

    private Integer travelTimeFromPrevSeconds;

    public Integer getDirectionId() {
        return directionId;
    }

    public void setDirectionId(Integer directionId) {
        this.directionId = directionId;
    }

    public Integer getStationId() {
        return stationId;
    }

    public void setStationId(Integer stationId) {
        this.stationId = stationId;
    }

    public Integer getStopSequence() {
        return stopSequence;
    }

    public void setStopSequence(Integer stopSequence) {
        this.stopSequence = stopSequence;
    }

    public Integer getTravelTimeFromPrevSeconds() {
        return travelTimeFromPrevSeconds;
    }

    public void setTravelTimeFromPrevSeconds(Integer travelTimeFromPrevSeconds) {
        this.travelTimeFromPrevSeconds = travelTimeFromPrevSeconds;
    }
}
