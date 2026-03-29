package ba.unsa.etf.pnwt.routingservice.importer.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ScrapedDirection {

    @JsonProperty("terminus_line_id")
    private Integer terminusLineId;

    private String code;
    private String name;

    @JsonProperty("direction")
    private String directionLabel;

    @JsonProperty("line_id")
    private Integer lineId;

    @JsonProperty("length_meters")
    private Double lengthMeters;

    @JsonProperty("can_delete")
    private Boolean canDelete;

    private List<ScrapedStation> stations = new ArrayList<>();
    private List<ScrapedRoutePoint> routePoints = new ArrayList<>();

    public Integer getTerminusLineId() {
        return terminusLineId;
    }

    public void setTerminusLineId(Integer terminusLineId) {
        this.terminusLineId = terminusLineId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDirectionLabel() {
        return directionLabel;
    }

    public void setDirectionLabel(String directionLabel) {
        this.directionLabel = directionLabel;
    }

    public Integer getLineId() {
        return lineId;
    }

    public void setLineId(Integer lineId) {
        this.lineId = lineId;
    }

    public Double getLengthMeters() {
        return lengthMeters;
    }

    public void setLengthMeters(Double lengthMeters) {
        this.lengthMeters = lengthMeters;
    }

    public Boolean getCanDelete() {
        return canDelete;
    }

    public void setCanDelete(Boolean canDelete) {
        this.canDelete = canDelete;
    }

    public List<ScrapedStation> getStations() {
        return stations;
    }

    public void setStations(List<ScrapedStation> stations) {
        this.stations = stations;
    }

    public List<ScrapedRoutePoint> getRoutePoints() {
        return routePoints;
    }

    public void setRoutePoints(List<ScrapedRoutePoint> routePoints) {
        this.routePoints = routePoints;
    }
}
