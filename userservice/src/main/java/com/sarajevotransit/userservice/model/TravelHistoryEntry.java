package com.sarajevotransit.userservice.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

@Entity
@Table(name = "travel_history_entries")
public class TravelHistoryEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull
    private UserProfile user;

    @Column(name = "line_code", nullable = false, length = 40)
    @NotBlank
    @Size(max = 40)
    private String lineCode;

    @Column(nullable = false)
    @NotBlank
    @Size(max = 120)
    private String fromStop;

    @Column(nullable = false)
    @NotBlank
    @Size(max = 120)
    private String toStop;

    @Column(nullable = false)
    @NotNull
    private LocalDateTime traveledAt;

    @Column(nullable = false)
    @NotNull
    @Min(1)
    private Integer durationMinutes;

    @PrePersist
    public void prePersist() {
        if (this.traveledAt == null) {
            this.traveledAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public UserProfile getUser() {
        return user;
    }

    public void setUser(UserProfile user) {
        this.user = user;
    }

    public String getLineCode() {
        return lineCode;
    }

    public void setLineCode(String lineCode) {
        this.lineCode = lineCode;
    }

    public String getFromStop() {
        return fromStop;
    }

    public void setFromStop(String fromStop) {
        this.fromStop = fromStop;
    }

    public String getToStop() {
        return toStop;
    }

    public void setToStop(String toStop) {
        this.toStop = toStop;
    }

    public LocalDateTime getTraveledAt() {
        return traveledAt;
    }

    public void setTraveledAt(LocalDateTime traveledAt) {
        this.traveledAt = traveledAt;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }
}
