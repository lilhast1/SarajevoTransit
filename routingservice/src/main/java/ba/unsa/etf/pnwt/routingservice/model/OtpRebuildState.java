package ba.unsa.etf.pnwt.routingservice.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "otp_rebuild_state")
public class OtpRebuildState {

    @Id
    private Long id = 1L;

    @Column(name = "needs_rebuild", nullable = false)
    private boolean needsRebuild = false;

    @Column(name = "rebuild_in_progress", nullable = false)
    private boolean rebuildInProgress = false;

    @Column(name = "last_data_change_at")
    private Instant lastDataChangeAt;

    @Column(name = "last_rebuild_at")
    private Instant lastRebuildAt;

    @Column(name = "rebuild_triggered_by", length = 100)
    private String rebuildTriggeredBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RebuildStatus status = RebuildStatus.IDLE;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public boolean isNeedsRebuild() {
        return needsRebuild;
    }

    public void setNeedsRebuild(boolean needsRebuild) {
        this.needsRebuild = needsRebuild;
    }

    public boolean isRebuildInProgress() {
        return rebuildInProgress;
    }

    public void setRebuildInProgress(boolean rebuildInProgress) {
        this.rebuildInProgress = rebuildInProgress;
    }

    public Instant getLastDataChangeAt() {
        return lastDataChangeAt;
    }

    public void setLastDataChangeAt(Instant lastDataChangeAt) {
        this.lastDataChangeAt = lastDataChangeAt;
    }

    public Instant getLastRebuildAt() {
        return lastRebuildAt;
    }

    public void setLastRebuildAt(Instant lastRebuildAt) {
        this.lastRebuildAt = lastRebuildAt;
    }

    public String getRebuildTriggeredBy() {
        return rebuildTriggeredBy;
    }

    public void setRebuildTriggeredBy(String rebuildTriggeredBy) {
        this.rebuildTriggeredBy = rebuildTriggeredBy;
    }

    public RebuildStatus getStatus() {
        return status;
    }

    public void setStatus(RebuildStatus status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public enum RebuildStatus {
        IDLE,
        BUILDING_GRAPH,
        STARTING_CONTAINER,
        HEALTH_CHECK,
        SWITCHING_TRAFFIC,
        DRAINING,
        COMPLETED,
        FAILED
    }
}
