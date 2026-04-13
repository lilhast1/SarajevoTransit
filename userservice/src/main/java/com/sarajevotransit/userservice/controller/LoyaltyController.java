package com.sarajevotransit.userservice.controller;

import com.sarajevotransit.userservice.dto.LoyaltyBalanceResponse;
import com.sarajevotransit.userservice.dto.LoyaltyEarnRequest;
import com.sarajevotransit.userservice.dto.LoyaltyRedeemRequest;
import com.sarajevotransit.userservice.dto.LoyaltyTransactionResponse;
import com.sarajevotransit.userservice.service.LoyaltyService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@Validated
@RequestMapping({ "/api/users/{userId}/loyalty", "/api/v1/users/{userId}/loyalty" })
public class LoyaltyController {

    private final LoyaltyService loyaltyService;

    public LoyaltyController(LoyaltyService loyaltyService) {
        this.loyaltyService = loyaltyService;
    }

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

    @PostMapping("/redeem")
    public ResponseEntity<LoyaltyBalanceResponse> redeem(
            @PathVariable @Positive Long userId,
            @Valid @RequestBody LoyaltyRedeemRequest request) {
        LoyaltyBalanceResponse created = loyaltyService.redeemPoints(userId, request);
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
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {
        return loyaltyService.getTransactions(userId, page, size, sort);
    }
}
