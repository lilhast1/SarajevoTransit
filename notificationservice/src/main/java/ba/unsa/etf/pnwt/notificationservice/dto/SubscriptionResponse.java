package ba.unsa.etf.pnwt.notificationservice.dto;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

public class SubscriptionResponse {

    private UUID id;
    private UUID userId;
    private String userFullName;
    private String userEmail;
    private UUID lineId;
    private String lineCode;
    private String lineName;
    private LocalTime startInterval;
    private LocalTime endInterval;
    private String daysOfWeek;
    private Boolean isActive;
    private LocalDateTime createdAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public String getUserFullName() { return userFullName; }
    public void setUserFullName(String userFullName) { this.userFullName = userFullName; }

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

    public UUID getLineId() { return lineId; }
    public void setLineId(UUID lineId) { this.lineId = lineId; }

    public String getLineCode() { return lineCode; }
    public void setLineCode(String lineCode) { this.lineCode = lineCode; }

    public String getLineName() { return lineName; }
    public void setLineName(String lineName) { this.lineName = lineName; }

    public LocalTime getStartInterval() { return startInterval; }
    public void setStartInterval(LocalTime startInterval) { this.startInterval = startInterval; }

    public LocalTime getEndInterval() { return endInterval; }
    public void setEndInterval(LocalTime endInterval) { this.endInterval = endInterval; }

    public String getDaysOfWeek() { return daysOfWeek; }
    public void setDaysOfWeek(String daysOfWeek) { this.daysOfWeek = daysOfWeek; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
