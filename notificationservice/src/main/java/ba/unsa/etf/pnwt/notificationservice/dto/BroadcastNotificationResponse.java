package ba.unsa.etf.pnwt.notificationservice.dto;

public class BroadcastNotificationResponse {

    private int notificationsCreated;
    private Long lineId;
    private String lineCode;
    private String lineName;

    public BroadcastNotificationResponse() {}

    public BroadcastNotificationResponse(int notificationsCreated, Long lineId, String lineCode, String lineName) {
        this.notificationsCreated = notificationsCreated;
        this.lineId = lineId;
        this.lineCode = lineCode;
        this.lineName = lineName;
    }

    public int getNotificationsCreated() { return notificationsCreated; }
    public void setNotificationsCreated(int notificationsCreated) { this.notificationsCreated = notificationsCreated; }

    public Long getLineId() { return lineId; }
    public void setLineId(Long lineId) { this.lineId = lineId; }

    public String getLineCode() { return lineCode; }
    public void setLineCode(String lineCode) { this.lineCode = lineCode; }

    public String getLineName() { return lineName; }
    public void setLineName(String lineName) { this.lineName = lineName; }
}
