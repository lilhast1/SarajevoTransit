package com.sarajevotransit.moneyman.controller;

import com.sarajevotransit.moneyman.dto.TicketPurchaseRequest;
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
@RequiredArgsConstructor
public class FinanceController {

    private final MoneymanService moneymanService;

    @PostMapping("/purchase")
    public ResponseEntity<Ticket> purchase(@Valid @RequestBody TicketPurchaseRequest request) {
        return ResponseEntity.ok(moneymanService.purchaseTicket(request));
    }

    @GetMapping("/health")
    public String health() {
        return "Finance service is up!";
    }

    @GetMapping("/wallet/{userId}")
    public ResponseEntity<List<Ticket>> getWallet(@PathVariable Long userId) {
        List<Ticket> activeTickets = moneymanService.getUserWallet(userId);

        if (activeTickets.isEmpty()) {
            return ResponseEntity.noContent().build(); // Return 204 if wallet is empty
        }

        return ResponseEntity.ok(activeTickets); // Return 200 with the list
    }
}