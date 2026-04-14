package ba.unsa.etf.pnwt.notificationservice.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "subscriptions")
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    private Long id;

    @NotNull
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "user_full_name", length = 255)
    private String userFullName;

    @Email
    @Column(name = "user_email", length = 255)
    private String userEmail;

    @NotNull
    @Column(name = "line_id", nullable = false)
    private Long lineId;

    @Column(name = "line_code", length = 20)
    private String lineCode;

    @Column(name = "line_name", length = 200)
    private String lineName;

    @Column(name = "start_interval")
    private LocalTime startInterval;

    @Column(name = "end_interval")
    private LocalTime endInterval;

    @Column(name = "days_of_week", length = 50)
    private String daysOfWeek;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
    }

    // Getteri i setteri
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUserFullName() { return userFullName; }
    public void setUserFullName(String userFullName) { this.userFullName = userFullName; }

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

    public Long getLineId() { return lineId; }
    public void setLineId(Long lineId) { this.lineId = lineId; }

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
