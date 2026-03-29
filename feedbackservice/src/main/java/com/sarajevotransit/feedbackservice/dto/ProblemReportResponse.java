package com.sarajevotransit.feedbackservice.dto;

import com.sarajevotransit.feedbackservice.model.ProblemCategory;
import com.sarajevotransit.feedbackservice.model.ReportStatus;

import java.time.OffsetDateTime;
import java.util.List;

public class ProblemReportResponse {

    private Long id;
    private Long reporterUserId;
    private String lineCode;
    private Long vehicleId;
    private String vehicleRegistrationNumber;
    private String vehicleInternalId;
    private String vehicleType;
    private String stationCode;
    private ProblemCategory category;
    private String description;
    private List<String> photoUrls;
    private ReportStatus status;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public ReportStatus getStatus() {
        return status;
    }

    public void setStatus(ReportStatus status) {
        this.status = status;
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
