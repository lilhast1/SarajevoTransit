package ba.unsa.etf.pnwt.routingservice.importer.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ScrapedTimetableEntry {

    private Integer id;
    private String name;

    @JsonProperty("terminus_line_id")
    private Integer terminusLineId;

    @JsonProperty("start_time")
    private String startTime;

    @JsonProperty("valid_from")
    private String validFrom;

    @JsonProperty("valid_to")
    private String validTo;

    @JsonProperty("rides_on_holidays")
    private Boolean ridesOnHolidays;

    @JsonProperty("days_of_week")
    private List<Short> daysOfWeek = new ArrayList<>();

    @JsonProperty("line_id")
    private Integer lineId;

    @JsonProperty("receives_passengers")
    private Boolean receivesPassengers;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getTerminusLineId() {
        return terminusLineId;
    }

    public void setTerminusLineId(Integer terminusLineId) {
        this.terminusLineId = terminusLineId;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(String validFrom) {
        this.validFrom = validFrom;
    }

    public String getValidTo() {
        return validTo;
    }

    public void setValidTo(String validTo) {
        this.validTo = validTo;
    }

    public Boolean getRidesOnHolidays() {
        return ridesOnHolidays;
    }

    public void setRidesOnHolidays(Boolean ridesOnHolidays) {
        this.ridesOnHolidays = ridesOnHolidays;
    }

    public List<Short> getDaysOfWeek() {
        return daysOfWeek;
    }

    public void setDaysOfWeek(List<Short> daysOfWeek) {
        this.daysOfWeek = daysOfWeek;
    }

    public Integer getLineId() {
        return lineId;
    }

    public void setLineId(Integer lineId) {
        this.lineId = lineId;
    }

    public Boolean getReceivesPassengers() {
        return receivesPassengers;
    }

    public void setReceivesPassengers(Boolean receivesPassengers) {
        this.receivesPassengers = receivesPassengers;
    }
}
