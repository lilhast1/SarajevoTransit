package com.sarajevotransit.userservice.dto;

import com.sarajevotransit.userservice.model.LoyaltyCouponType;
import com.sarajevotransit.userservice.model.LoyaltyTier;

public record CouponApplicationResponse(
        String couponCode,
        LoyaltyCouponType couponType,
        LoyaltyTier tier,
        int discountPercent,
        String rideCode,
        boolean freeRide) {
}