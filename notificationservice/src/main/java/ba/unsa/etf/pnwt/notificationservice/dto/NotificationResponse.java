package ba.unsa.etf.pnwt.notificationservice.dto;

import ba.unsa.etf.pnwt.notificationservice.model.NotificationType;

import java.time.LocalDateTime;
import java.util.UUID;

public class NotificationResponse {

    private UUID id;
    private UUID userId;
    private String userFullName;
    private String userEmail;
    private UUID lineId;
    private String lineCode;
    private String lineName;
    private NotificationType type;
    private String title;
    private String content;
    private Boolean isRead;
    private LocalDateTime sentAt;

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

    public NotificationType getType() { return type; }
    public void setType(NotificationType type) { this.type = type; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Boolean getIsRead() { return isRead; }
    public void setIsRead(Boolean isRead) { this.isRead = isRead; }

    public LocalDateTime getSentAt() { return sentAt; }
    public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }
}
