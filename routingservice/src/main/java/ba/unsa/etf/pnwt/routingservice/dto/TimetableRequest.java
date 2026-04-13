package ba.unsa.etf.pnwt.routingservice.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public class TimetableRequest {

    private Integer externalId;

    @NotNull
    private Integer directionId;

    @NotNull
    private Integer lineId;

    @Size(max = 200)
    private String name;

    @NotNull
    private LocalTime departureTime;

    private LocalDate validFrom;
    private LocalDate validTo;

    @NotNull
    private Boolean ridesOnHolidays;

    @NotEmpty
    private List<Short> daysOfWeek;

    @NotNull
    private Boolean receivesPassengers;

    @NotNull
    private Boolean isActive;

    public Integer getExternalId() {
        return externalId;
    }

    public void setExternalId(Integer externalId) {
        this.externalId = externalId;
    }

    public Integer getDirectionId() {
        return directionId;
    }

    public void setDirectionId(Integer directionId) {
        this.directionId = directionId;
    }

    public Integer getLineId() {
        return lineId;
    }

    public void setLineId(Integer lineId) {
        this.lineId = lineId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalTime getDepartureTime() {
        return departureTime;
    }

    public void setDepartureTime(LocalTime departureTime) {
        this.departureTime = departureTime;
    }

    public LocalDate getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(LocalDate validFrom) {
        this.validFrom = validFrom;
    }

    public LocalDate getValidTo() {
        return validTo;
    }

    public void setValidTo(LocalDate validTo) {
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

    public Boolean getReceivesPassengers() {
        return receivesPassengers;
    }

    public void setReceivesPassengers(Boolean receivesPassengers) {
        this.receivesPassengers = receivesPassengers;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
}
