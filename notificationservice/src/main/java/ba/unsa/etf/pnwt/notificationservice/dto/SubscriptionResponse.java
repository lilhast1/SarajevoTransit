package ba.unsa.etf.pnwt.notificationservice.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
public class SubscriptionResponse {

    private Long id;
    private Long userId;
    private String userFullName;
    private String userEmail;
    private Long lineId;
    private String lineCode;
    private String lineName;
    private LocalTime startInterval;
    private LocalTime endInterval;
    private String daysOfWeek;
    private Boolean isActive;
    private LocalDateTime createdAt;
}
