package ba.unsa.etf.pnwt.routingservice.importer.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ScrapedTimetableGroup {

    private Integer id;
    private String code;
    private String name;
    private List<ScrapedTimetableEntry> timetable = new ArrayList<>();

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
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

    public List<ScrapedTimetableEntry> getTimetable() {
        return timetable;
    }

    public void setTimetable(List<ScrapedTimetableEntry> timetable) {
        this.timetable = timetable;
    }
}
