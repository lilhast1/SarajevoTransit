package ba.unsa.etf.pnwt.notificationservice.dto;

public record NotificationInstanceResponse(
        String serviceName,
        String instanceId,
        String host,
        int port,
        String timestamp) {}
