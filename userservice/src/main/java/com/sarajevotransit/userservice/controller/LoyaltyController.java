package com.sarajevotransit.userservice.controller;

import com.sarajevotransit.userservice.dto.LoyaltyBalanceResponse;
import com.sarajevotransit.userservice.dto.ApplyCouponRequest;
import com.sarajevotransit.userservice.dto.CouponApplicationResponse;
import com.sarajevotransit.userservice.dto.GenerateLoyaltyCouponRequest;
import com.sarajevotransit.userservice.dto.LoyaltyEarnRequest;
import com.sarajevotransit.userservice.dto.LoyaltyCouponResponse;
import com.sarajevotransit.userservice.dto.LoyaltyTransactionResponse;
import com.sarajevotransit.userservice.dto.PaginationRequest;
import com.sarajevotransit.userservice.service.LoyaltyService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping({ "/api/users/{userId}/loyalty", "/api/v1/users/{userId}/loyalty" })
@SecurityRequirement(name = "bearerAuth")
public class LoyaltyController {

    private final LoyaltyService loyaltyService;

    @PostMapping("/earn")
    public ResponseEntity<LoyaltyBalanceResponse> earn(
            @PathVariable @Positive Long userId,
            @Valid @RequestBody LoyaltyEarnRequest request) {
        LoyaltyBalanceResponse created = loyaltyService.earnPoints(userId, request);
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/users/{userId}/loyalty/balance")
                .buildAndExpand(userId)
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping("/balance")
    public LoyaltyBalanceResponse getBalance(@PathVariable @Positive Long userId) {
        return loyaltyService.getBalance(userId);
    }

    @GetMapping("/transactions")
    public Page<LoyaltyTransactionResponse> getTransactions(
            @PathVariable @Positive Long userId,
            @Valid PaginationRequest request) {
        return loyaltyService.getTransactions(userId, request.getPage(), request.getSize(), request.getSort());
    }

    @PostMapping("/coupons")
    public LoyaltyCouponResponse generateCoupon(
            @PathVariable @Positive Long userId,
            @Valid @RequestBody GenerateLoyaltyCouponRequest request) {
        return loyaltyService.generateCoupon(userId, request);
    }

    @GetMapping("/coupons")
    public java.util.List<LoyaltyCouponResponse> getCoupons(@PathVariable @Positive Long userId) {
        return loyaltyService.getCoupons(userId);
    }

    @PostMapping("/coupons/{couponCode}/apply")
    public CouponApplicationResponse applyCoupon(
            @PathVariable @Positive Long userId,
            @PathVariable String couponCode,
            @Valid @RequestBody(required = false) ApplyCouponRequest request) {
        return loyaltyService.applyCoupon(userId, couponCode, request);
    }
}
