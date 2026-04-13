package ba.unsa.etf.pnwt.notificationservice.dto;

import ba.unsa.etf.pnwt.notificationservice.model.NotificationType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public class CreateNotificationRequest {

    @NotNull
    private UUID userId;

    @Size(max = 255)
    private String userFullName;

    @Email
    @Size(max = 255)
    private String userEmail;

    private UUID lineId;

    @Size(max = 20)
    private String lineCode;

    @Size(max = 200)
    private String lineName;

    @NotNull
    private NotificationType type;

    @NotBlank
    @Size(max = 255)
    private String title;

    @NotBlank
    private String content;

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
}
