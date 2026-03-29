package com.sarajevotransit.userservice.controller;

import com.sarajevotransit.userservice.dto.LoyaltyBalanceResponse;
import com.sarajevotransit.userservice.dto.LoyaltyEarnRequest;
import com.sarajevotransit.userservice.dto.LoyaltyRedeemRequest;
import com.sarajevotransit.userservice.dto.LoyaltyTransactionResponse;
import com.sarajevotransit.userservice.service.LoyaltyService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users/{userId}/loyalty")
public class LoyaltyController {

    private final LoyaltyService loyaltyService;

    public LoyaltyController(LoyaltyService loyaltyService) {
        this.loyaltyService = loyaltyService;
    }

    @PostMapping("/earn")
    public ResponseEntity<LoyaltyBalanceResponse> earn(
            @PathVariable Long userId,
            @Valid @RequestBody LoyaltyEarnRequest request) {
        return ResponseEntity.ok(loyaltyService.earnPoints(userId, request));
    }

    @PostMapping("/redeem")
    public ResponseEntity<LoyaltyBalanceResponse> redeem(
            @PathVariable Long userId,
            @Valid @RequestBody LoyaltyRedeemRequest request) {
        return ResponseEntity.ok(loyaltyService.redeemPoints(userId, request));
    }

    @GetMapping("/balance")
    public LoyaltyBalanceResponse getBalance(@PathVariable Long userId) {
        return loyaltyService.getBalance(userId);
    }

    @GetMapping("/transactions")
    public List<LoyaltyTransactionResponse> getTransactions(@PathVariable Long userId) {
        return loyaltyService.getTransactions(userId);
    }
}
