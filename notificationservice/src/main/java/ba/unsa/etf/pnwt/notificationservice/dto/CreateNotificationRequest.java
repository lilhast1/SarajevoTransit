package ba.unsa.etf.pnwt.notificationservice.dto;

import ba.unsa.etf.pnwt.notificationservice.model.NotificationType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateNotificationRequest {

    @NotNull
    private Long userId;

    @Size(max = 255)
    private String userFullName;

    @Email
    @Size(max = 255)
    private String userEmail;

    private Long lineId;

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
}
