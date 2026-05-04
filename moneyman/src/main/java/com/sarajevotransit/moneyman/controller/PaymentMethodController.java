package com.sarajevotransit.moneyman.controller;

import com.sarajevotransit.moneyman.dto.PaymentMethodRequest;
import com.sarajevotransit.moneyman.dto.PaymentMethodResponse;
import com.sarajevotransit.moneyman.model.PaymentMethod;
import com.sarajevotransit.moneyman.repository.PaymentMethodRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/payments/methods")
@Tag(name = "Payment Methods", description = "Endpoints to manage user payment methods")
public class PaymentMethodController {

    private final PaymentMethodRepository repository;

    public PaymentMethodController(PaymentMethodRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/{userId}")
    @Operation(summary = "List payment methods", description = "Retrieve saved payment methods for a user with pagination and sorting")
    public Page<PaymentMethodResponse> getMethods(@PathVariable Long userId, @PageableDefault(size = 15) Pageable pageable) {
        return repository.findByUserId(userId, pageable)
                .map(this::toResponse);
    }

    @PostMapping
    @Operation(summary = "Add payment method", description = "Save a new payment method for a user")
    public ResponseEntity<PaymentMethodResponse> addMethod(@Valid @RequestBody PaymentMethodRequest request) {
        PaymentMethod method = new PaymentMethod();
        method.setUserId(request.getUserId());
        method.setProvider(request.getProvider());
        method.setGatewayToken(request.getGatewayToken());
        method.setLastFour(request.getLastFour());
        method.setCardType(request.getCardType());
        method.setDefault(request.isDefault());

        PaymentMethod saved = repository.save(method);
        return ResponseEntity.ok(toResponse(saved));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> removeMethod(@PathVariable Long id) {
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private PaymentMethodResponse toResponse(PaymentMethod method) {
        return PaymentMethodResponse.builder()
                .id(method.getId())
                .userId(method.getUserId())
                .provider(method.getProvider())
                .lastFour(method.getLastFour())
                .cardType(method.getCardType())
                .isDefault(method.isDefault())
                .build();
    }
}