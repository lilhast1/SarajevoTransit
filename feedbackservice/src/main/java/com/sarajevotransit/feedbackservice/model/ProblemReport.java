package com.sarajevotransit.feedbackservice.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "incident_reports", indexes = {
        @Index(name = "idx_incident_reports_status_created", columnList = "status, createdAt"),
        @Index(name = "idx_incident_reports_user_created", columnList = "user_id, createdAt"),
        @Index(name = "idx_incident_reports_line", columnList = "line_id"),
        @Index(name = "idx_incident_reports_station", columnList = "station_id"),
        @Index(name = "idx_incident_reports_vehicle", columnList = "vehicle_id")
})
@Getter
@Setter
@NoArgsConstructor
public class ProblemReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_id")
    private Long id;

    @NotNull
    @Positive
    @Column(name = "user_id", nullable = false)
    private Long reporterUserId;

    @Positive
    @Column(name = "line_id")
    private Long lineId;

    @Positive
    @Column(name = "vehicle_id")
    private Long vehicleId;

    @Size(max = 60)
    @Column(name = "vehicle_registration_number", length = 60)
    private String vehicleRegistrationNumber;

    @Size(max = 60)
    @Column(name = "vehicle_internal_id", length = 60)
    private String vehicleInternalId;

    @Size(max = 30)
    @Column(name = "vehicle_type", length = 30)
    private String vehicleType;

    @Positive
    @Column(name = "station_id")
    private Long stationId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private ProblemCategory category;

    @NotBlank
    @Size(max = 1000)
    @Column(nullable = false, length = 1000)
    private String description;

    @ElementCollection
    @CollectionTable(name = "incident_report_photos", joinColumns = @JoinColumn(name = "incident_report_id"))
    @Column(name = "photo_url", length = 500)
    private List<@Size(max = 500) String> photoUrls = new ArrayList<>();

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReportStatus status = ReportStatus.RECEIVED;

    @NotNull
    @Column(nullable = false)
    private OffsetDateTime createdAt;

    @NotNull
    @Column(nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    public void setPhotoUrls(List<String> photoUrls) {
        this.photoUrls = photoUrls == null ? new ArrayList<>() : new ArrayList<>(photoUrls);
    }
}