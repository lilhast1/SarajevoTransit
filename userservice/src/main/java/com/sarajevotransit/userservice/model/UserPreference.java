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
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_preferences")
@Getter
@Setter
public class UserPreference {

    @Id
    private Long userId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @NotNull
    private UserProfile user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @NotNull
    private LanguageCode languageCode = LanguageCode.BS;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @NotNull
    private ThemeMode themeMode = ThemeMode.SYSTEM;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @NotNull
    private NotificationChannel notificationChannel = NotificationChannel.PUSH;

    @Column(nullable = false)
    @NotNull
    private Boolean highContrastEnabled = false;

    @Column(nullable = false)
    @NotNull
    private Boolean largeTextEnabled = false;

    @Column(nullable = false)
    @NotNull
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
}
