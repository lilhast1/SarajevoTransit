package ba.unsa.etf.pnwt.notificationservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BroadcastNotificationResponse {

    private int notificationsCreated;
    private Long lineId;
    private String lineCode;
    private String lineName;
}
