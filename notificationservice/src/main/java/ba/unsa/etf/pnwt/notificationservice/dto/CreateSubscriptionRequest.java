package ba.unsa.etf.pnwt.notificationservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalTime;

@Data
public class CreateSubscriptionRequest {

    @NotNull
    private Long userId;

    @Size(max = 255)
    private String userFullName;

    @Email
    @Size(max = 255)
    private String userEmail;

    @NotNull
    private Long lineId;

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
}
