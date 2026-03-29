package com.sarajevotransit.userservice.dto;

import com.sarajevotransit.userservice.model.LanguageCode;
import com.sarajevotransit.userservice.model.NotificationChannel;
import com.sarajevotransit.userservice.model.ThemeMode;

import java.time.LocalDateTime;

public record UserPreferenceResponse(
        LanguageCode languageCode,
        ThemeMode themeMode,
        NotificationChannel notificationChannel,
        boolean highContrastEnabled,
        boolean largeTextEnabled,
        boolean screenReaderEnabled,
        LocalDateTime updatedAt) {
}
