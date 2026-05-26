package com.sarajevotransit.moneyman.dto;

public record CouponApplicationResponse(
        String couponCode,
        String couponType,
        String tier,
        int discountPercent,
        String rideCode,
        boolean freeRide) {
}