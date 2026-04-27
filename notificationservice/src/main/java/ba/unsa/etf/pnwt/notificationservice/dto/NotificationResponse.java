package ba.unsa.etf.pnwt.notificationservice.dto;

import ba.unsa.etf.pnwt.notificationservice.model.NotificationType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NotificationResponse {

    private Long id;
    private Long userId;
    private String userFullName;
    private String userEmail;
    private Long lineId;
    private String lineCode;
    private String lineName;
    private NotificationType type;
    private String title;
    private String content;
    private Boolean isRead;
    private LocalDateTime sentAt;
}
