package com.sarajevotransit.userservice.dto;

import com.sarajevotransit.userservice.model.LoyaltyCouponType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record GenerateLoyaltyCouponRequest(
        @NotNull LoyaltyCouponType couponType,
        @Size(max = 32) String rideCode) {
}