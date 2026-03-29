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
    private Long reporterUserId;

    @Size(max = 40)
    private String lineCode;

    @Positive
    private Long vehicleId;

    @Size(max = 60)
    private String vehicleRegistrationNumber;

    @Size(max = 60)
    private String vehicleInternalId;

    @Size(max = 30)
    private String vehicleType;

    @Size(max = 60)
    private String stationCode;

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

    public String getLineCode() {
        return lineCode;
    }

    public void setLineCode(String lineCode) {
        this.lineCode = lineCode;
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

    public String getStationCode() {
        return stationCode;
    }

    public void setStationCode(String stationCode) {
        this.stationCode = stationCode;
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
