package com.sarajevotransit.userservice.dto;

import jakarta.validation.constraints.Size;

public record ApplyCouponRequest(
        @Size(max = 32) String rideCode) {
}