package com.sarajevotransit.userservice.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_preferences")
public class UserPreference {

    @Id
    private Long userId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserProfile user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LanguageCode languageCode = LanguageCode.BS;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ThemeMode themeMode = ThemeMode.SYSTEM;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationChannel notificationChannel = NotificationChannel.PUSH;

    @Column(nullable = false)
    private Boolean highContrastEnabled = false;

    @Column(nullable = false)
    private Boolean largeTextEnabled = false;

    @Column(nullable = false)
    private Boolean screenReaderEnabled = false;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public UserProfile getUser() {
        return user;
    }

    public void setUser(UserProfile user) {
        this.user = user;
    }

    public LanguageCode getLanguageCode() {
        return languageCode;
    }

    public void setLanguageCode(LanguageCode languageCode) {
        this.languageCode = languageCode;
    }

    public ThemeMode getThemeMode() {
        return themeMode;
    }

    public void setThemeMode(ThemeMode themeMode) {
        this.themeMode = themeMode;
    }

    public NotificationChannel getNotificationChannel() {
        return notificationChannel;
    }

    public void setNotificationChannel(NotificationChannel notificationChannel) {
        this.notificationChannel = notificationChannel;
    }

    public Boolean getHighContrastEnabled() {
        return highContrastEnabled;
    }

    public void setHighContrastEnabled(Boolean highContrastEnabled) {
        this.highContrastEnabled = highContrastEnabled;
    }

    public Boolean getLargeTextEnabled() {
        return largeTextEnabled;
    }

    public void setLargeTextEnabled(Boolean largeTextEnabled) {
        this.largeTextEnabled = largeTextEnabled;
    }

    public Boolean getScreenReaderEnabled() {
        return screenReaderEnabled;
    }

    public void setScreenReaderEnabled(Boolean screenReaderEnabled) {
        this.screenReaderEnabled = screenReaderEnabled;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
