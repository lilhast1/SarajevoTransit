package ba.unsa.etf.pnwt.notificationservice.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalTime;

@Data
public class UpdateSubscriptionRequest {

    private Long lineId;

    @Size(max = 20)
    private String lineCode;

    @Size(max = 200)
    private String lineName;

    private LocalTime startInterval;

    private LocalTime endInterval;

    @Size(max = 50)
    private String daysOfWeek;
}
