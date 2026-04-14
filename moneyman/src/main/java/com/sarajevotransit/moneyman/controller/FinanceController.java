package com.sarajevotransit.moneyman.controller;

import com.sarajevotransit.moneyman.dto.TicketPurchaseRequest;
import com.sarajevotransit.moneyman.dto.TicketResponseDTO;
import com.sarajevotransit.moneyman.model.Ticket;
import com.sarajevotransit.moneyman.model.enums.TicketStatus;
import com.sarajevotransit.moneyman.service.MoneymanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/finance")
//@RequiredArgsConstructor
public class FinanceController {

    private final MoneymanService moneymanService;

    public FinanceController(MoneymanService moneymanService) {
        this.moneymanService = moneymanService;
    }
    @PostMapping("/purchase")
    public ResponseEntity<Ticket> purchase(@Valid @RequestBody TicketPurchaseRequest request) {
        return ResponseEntity.ok(moneymanService.purchaseTicket(request));
    }

    @GetMapping("/health")
    public String health() {
        return "Finance service is up!";
    }

    @GetMapping("/wallet/{userId}")
    public ResponseEntity<List<TicketResponseDTO>> getWallet(@PathVariable Long userId) {
        // This calls the service which should use the mapper
        List<TicketResponseDTO> wallet = moneymanService.getUserWallet(userId);
        return ResponseEntity.ok(wallet);
    }
}