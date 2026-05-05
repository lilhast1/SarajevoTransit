package com.sarajevotransit.userservice.dto.notification;

public class CreateNotificationRequest {
    private Long userId;
    private String userFullName;
    private String userEmail;
    private String type;
    private String title;
    private String content;

    public CreateNotificationRequest() {}

    public CreateNotificationRequest(Long userId, String userFullName, String userEmail, String type, String title, String content) {
        this.userId = userId;
        this.userFullName = userFullName;
        this.userEmail = userEmail;
        this.type = type;
        this.title = title;
        this.content = content;
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUserFullName() { return userFullName; }
    public void setUserFullName(String userFullName) { this.userFullName = userFullName; }
    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}