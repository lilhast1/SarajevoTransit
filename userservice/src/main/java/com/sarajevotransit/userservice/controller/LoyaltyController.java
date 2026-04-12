package com.sarajevotransit.userservice.controller;

import com.sarajevotransit.userservice.dto.LoyaltyBalanceResponse;
import com.sarajevotransit.userservice.dto.LoyaltyEarnRequest;
import com.sarajevotransit.userservice.dto.LoyaltyRedeemRequest;
import com.sarajevotransit.userservice.dto.LoyaltyTransactionResponse;
import com.sarajevotransit.userservice.service.LoyaltyService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
        return ResponseEntity.ok(loyaltyService.earnPoints(userId, request));
    }

    @PostMapping("/redeem")
    public ResponseEntity<LoyaltyBalanceResponse> redeem(
            @PathVariable @Positive Long userId,
            @Valid @RequestBody LoyaltyRedeemRequest request) {
        return ResponseEntity.ok(loyaltyService.redeemPoints(userId, request));
    }

    @GetMapping("/balance")
    public LoyaltyBalanceResponse getBalance(@PathVariable @Positive Long userId) {
        return loyaltyService.getBalance(userId);
    }

    @GetMapping("/transactions")
    public List<LoyaltyTransactionResponse> getTransactions(@PathVariable @Positive Long userId) {
        return loyaltyService.getTransactions(userId);
    }
}
