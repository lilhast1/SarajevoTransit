package com.sarajevotransit.moneyman.controller;

import com.sarajevotransit.moneyman.dto.TicketPurchaseRequest;
import com.sarajevotransit.moneyman.dto.TicketResponseDTO;
import com.sarajevotransit.moneyman.mapper.MoneymanMapper;
import com.sarajevotransit.moneyman.model.Ticket;
import com.sarajevotransit.moneyman.service.MoneymanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/finance")
@Tag(name = "Finance", description = "Endpoints for ticket purchases and wallet management")
public class FinanceController {

    private final MoneymanMapper moneymanMapper;
    private final MoneymanService moneymanService;

    public FinanceController(MoneymanService moneymanService, MoneymanMapper moneymanMapper) {
        this.moneymanService = moneymanService;
        this.moneymanMapper = moneymanMapper;
    }

    @PostMapping("/purchase")
    @Operation(summary = "Purchase ticket", description = "Buy a new ticket and add it to the user's digital wallet")
    public ResponseEntity<TicketResponseDTO> purchase(@Valid @RequestBody TicketPurchaseRequest request) {
        Ticket ticket = moneymanService.purchaseTicket(request);
        var response = moneymanMapper.toResponseDTO(ticket);
        return ResponseEntity.ok(response);
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