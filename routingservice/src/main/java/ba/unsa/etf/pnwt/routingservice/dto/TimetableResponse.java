package ba.unsa.etf.pnwt.routingservice.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;

public class TimetableResponse {

    private Integer id;
    private Integer externalId;
    private Integer directionId;
    private Integer directionExternalId;
    private String directionName;
    private Integer lineId;
    private Integer lineExternalId;
    private String lineName;
    private String name;
    private LocalTime departureTime;
    private LocalDate validFrom;
    private LocalDate validTo;
    private Boolean ridesOnHolidays;
    private List<Short> daysOfWeek;
    private Boolean receivesPassengers;
    private Boolean isActive;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

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

    public Integer getLineId() {
        return lineId;
    }

    public void setLineId(Integer lineId) {
        this.lineId = lineId;
    }

    public Integer getLineExternalId() {
        return lineExternalId;
    }

    public void setLineExternalId(Integer lineExternalId) {
        this.lineExternalId = lineExternalId;
    }

    public String getLineName() {
        return lineName;
    }

    public void setLineName(String lineName) {
        this.lineName = lineName;
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

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
