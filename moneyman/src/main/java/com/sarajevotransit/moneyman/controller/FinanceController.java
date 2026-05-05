package com.sarajevotransit.moneyman.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchException;
import com.sarajevotransit.moneyman.dto.TicketPurchaseRequest;
import com.sarajevotransit.moneyman.dto.TicketResponseDTO;
import com.sarajevotransit.moneyman.mapper.MoneymanMapper;
import com.sarajevotransit.moneyman.model.Ticket;
import com.sarajevotransit.moneyman.service.MoneymanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

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
    @Operation(summary = "Get user wallet", description = "Retrieve user's tickets with pagination and sorting")
    public ResponseEntity<Page<TicketResponseDTO>> getWallet(@PathVariable Long userId,
            @PageableDefault(size = 15) Pageable pageable) {
        Page<TicketResponseDTO> wallet = moneymanService.getUserWallet(userId, pageable);
        return ResponseEntity.ok(wallet);
    }

    @PatchMapping("/tickets/{id}")
    @Operation(summary = "Update ticket", description = "Partially update ticket status using JsonPatch")
    public ResponseEntity<TicketResponseDTO> updateTicket(@PathVariable UUID id, @RequestBody JsonPatch patch) {
        try {
            Ticket updated = moneymanService.updateTicket(id, patch);
            TicketResponseDTO response = moneymanMapper.toResponseDTO(updated);
            return ResponseEntity.ok(response);
        } catch (JsonPatchException | IllegalArgumentException | JsonProcessingException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}