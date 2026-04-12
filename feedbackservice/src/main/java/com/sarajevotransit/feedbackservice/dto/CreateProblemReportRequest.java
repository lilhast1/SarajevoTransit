package com.sarajevotransit.feedbackservice.dto;

import com.sarajevotransit.feedbackservice.model.ProblemCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;

public class CreateProblemReportRequest {

    @NotNull
    @Positive
    private Long reporterUserId;

    @Positive
    private Long lineId;

    @Positive
    private Long vehicleId;

    @Size(max = 60)
    private String vehicleRegistrationNumber;

    @Size(max = 60)
    private String vehicleInternalId;

    @Size(max = 30)
    private String vehicleType;

    @Positive
    private Long stationId;

    @NotNull
    private ProblemCategory category;

    @NotBlank
    @Size(max = 1000)
    private String description;

    private List<@Size(max = 500) String> photoUrls = new ArrayList<>();

    public Long getReporterUserId() {
        return reporterUserId;
    }

    public void setReporterUserId(Long reporterUserId) {
        this.reporterUserId = reporterUserId;
    }

    public Long getLineId() {
        return lineId;
    }

    public void setLineId(Long lineId) {
        this.lineId = lineId;
    }

    public Long getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(Long vehicleId) {
        this.vehicleId = vehicleId;
    }

    public String getVehicleRegistrationNumber() {
        return vehicleRegistrationNumber;
    }

    public void setVehicleRegistrationNumber(String vehicleRegistrationNumber) {
        this.vehicleRegistrationNumber = vehicleRegistrationNumber;
    }

    public String getVehicleInternalId() {
        return vehicleInternalId;
    }

    public void setVehicleInternalId(String vehicleInternalId) {
        this.vehicleInternalId = vehicleInternalId;
    }

    public String getVehicleType() {
        return vehicleType;
    }

    public void setVehicleType(String vehicleType) {
        this.vehicleType = vehicleType;
    }

    public Long getStationId() {
        return stationId;
    }

    public void setStationId(Long stationId) {
        this.stationId = stationId;
    }

    public ProblemCategory getCategory() {
        return category;
    }

    public void setCategory(ProblemCategory category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getPhotoUrls() {
        return photoUrls;
    }

    public void setPhotoUrls(List<String> photoUrls) {
        this.photoUrls = photoUrls;
    }
}
