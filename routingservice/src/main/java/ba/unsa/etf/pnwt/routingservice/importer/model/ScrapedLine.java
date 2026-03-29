package ba.unsa.etf.pnwt.routingservice.importer.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ScrapedLine {

    private Integer id;
    private Short vehicleTypeId;
    private String code;
    private String name;
    private List<ScrapedDirection> directions = new ArrayList<>();
    private List<ScrapedTimetableGroup> timetableGroups = new ArrayList<>();

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Short getVehicleTypeId() {
        return vehicleTypeId;
    }

    public void setVehicleTypeId(Short vehicleTypeId) {
        this.vehicleTypeId = vehicleTypeId;
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

    public List<ScrapedDirection> getDirections() {
        return directions;
    }

    public void setDirections(List<ScrapedDirection> directions) {
        this.directions = directions;
    }

    public List<ScrapedTimetableGroup> getTimetableGroups() {
        return timetableGroups;
    }

    public void setTimetableGroups(List<ScrapedTimetableGroup> timetableGroups) {
        this.timetableGroups = timetableGroups;
    }
}
