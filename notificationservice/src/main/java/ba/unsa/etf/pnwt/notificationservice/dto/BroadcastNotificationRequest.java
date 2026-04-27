package ba.unsa.etf.pnwt.notificationservice.dto;

import ba.unsa.etf.pnwt.notificationservice.model.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class BroadcastNotificationRequest {

    @NotNull
    private Long lineId;

    @NotNull
    private NotificationType type;

    @NotBlank
    @Size(max = 255)
    private String title;

    @NotBlank
    private String content;

    public Long getLineId() { return lineId; }
    public void setLineId(Long lineId) { this.lineId = lineId; }

    public NotificationType getType() { return type; }
    public void setType(NotificationType type) { this.type = type; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}
