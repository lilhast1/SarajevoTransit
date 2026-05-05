package ba.unsa.etf.pnwt.routingservice.dto;

public class DirectionStationResponse {

    private Integer id;
    private Integer directionId;
    private Integer directionExternalId;
    private String directionName;
    private Integer stationId;
    private Integer stationExternalId;
    private String stationName;
    private Integer stopSequence;
    private Integer travelTimeFromPrevSeconds;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getDirectionId() {
        return directionId;
    }

    public void setDirectionId(Integer directionId) {
        this.directionId = directionId;
    }

    public Integer getDirectionExternalId() {
        return directionExternalId;
    }

    public void setDirectionExternalId(Integer directionExternalId) {
        this.directionExternalId = directionExternalId;
    }

    public String getDirectionName() {
        return directionName;
    }

    public void setDirectionName(String directionName) {
        this.directionName = directionName;
    }

    public Integer getStationId() {
        return stationId;
    }

    public void setStationId(Integer stationId) {
        this.stationId = stationId;
    }

    public Integer getStationExternalId() {
        return stationExternalId;
    }

    public void setStationExternalId(Integer stationExternalId) {
        this.stationExternalId = stationExternalId;
    }

    public String getStationName() {
        return stationName;
    }

    public void setStationName(String stationName) {
        this.stationName = stationName;
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
