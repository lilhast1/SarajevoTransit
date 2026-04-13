package ba.unsa.etf.pnwt.notificationservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalTime;
import java.util.UUID;

public class CreateSubscriptionRequest {

    @NotNull
    private UUID userId;

    @Size(max = 255)
    private String userFullName;

    @Email
    @Size(max = 255)
    private String userEmail;

    @NotNull
    private UUID lineId;

    @Size(max = 20)
    private String lineCode;

    @Size(max = 200)
    private String lineName;

    @NotNull
    private LocalTime startInterval;

    @NotNull
    private LocalTime endInterval;

    @Size(max = 50)
    private String daysOfWeek;

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
}
