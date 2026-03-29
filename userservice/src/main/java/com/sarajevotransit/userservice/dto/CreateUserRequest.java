package com.sarajevotransit.userservice.dto;

import com.sarajevotransit.userservice.model.LanguageCode;
import com.sarajevotransit.userservice.model.NotificationChannel;
import com.sarajevotransit.userservice.model.ThemeMode;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @NotBlank(message = "Full name is required") String fullName,
        @Email(message = "Email format is invalid") @NotBlank(message = "Email is required") String email,
        @NotBlank(message = "Password is required") @Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters") String password,
        LanguageCode languageCode,
        ThemeMode themeMode,
        NotificationChannel notificationChannel) {
}
