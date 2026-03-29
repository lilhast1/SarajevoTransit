package com.sarajevotransit.userservice.dto;

import com.sarajevotransit.userservice.model.LanguageCode;
import com.sarajevotransit.userservice.model.NotificationChannel;
import com.sarajevotransit.userservice.model.ThemeMode;
import jakarta.validation.constraints.NotNull;

public record UpdateUserPreferenceRequest(
        @NotNull(message = "Language is required") LanguageCode languageCode,
        @NotNull(message = "Theme is required") ThemeMode themeMode,
        @NotNull(message = "Notification channel is required") NotificationChannel notificationChannel,
        boolean highContrastEnabled,
        boolean largeTextEnabled,
        boolean screenReaderEnabled) {
}
