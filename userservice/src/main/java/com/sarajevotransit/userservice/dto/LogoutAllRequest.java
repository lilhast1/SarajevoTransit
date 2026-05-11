package com.sarajevotransit.userservice.dto;

import jakarta.validation.constraints.NotBlank;

public record LogoutAllRequest(
        @NotBlank(message = "Access token is required") String accessToken) {
}
