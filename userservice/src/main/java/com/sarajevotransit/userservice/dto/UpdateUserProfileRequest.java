package com.sarajevotransit.userservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UpdateUserProfileRequest(
        @NotBlank(message = "Full name is required") String fullName,
        @Email(message = "Email format is invalid") @NotBlank(message = "Email is required") String email) {
}
