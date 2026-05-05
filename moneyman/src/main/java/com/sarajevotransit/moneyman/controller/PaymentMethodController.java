package com.sarajevotransit.moneyman.controller;

import com.sarajevotransit.moneyman.dto.PaymentMethodRequest;
import com.sarajevotransit.moneyman.dto.PaymentMethodResponse;
import com.sarajevotransit.moneyman.model.PaymentMethod;
import com.sarajevotransit.moneyman.repository.PaymentMethodRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
<<<<<<< Updated upstream
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
=======
import org.springframework.http.HttpStatus;
>>>>>>> Stashed changes
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

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
<<<<<<< Updated upstream
    @Operation(summary = "List payment methods", description = "Retrieve saved payment methods for a user with pagination and sorting")
    public Page<PaymentMethodResponse> getMethods(@PathVariable Long userId, @PageableDefault(size = 15) Pageable pageable) {
        return repository.findByUserId(userId, pageable)
                .map(this::toResponse);
=======
    @Operation(summary = "List payment methods", description = "Retrieve saved payment methods for a user")
    public List<PaymentMethodResponse> getMethods(
            @PathVariable Long userId,
            HttpServletRequest httpRequest) {
        requireOwnerOrAdmin(httpRequest, userId);
        return repository.findByUserId(userId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
>>>>>>> Stashed changes
    }

    @PostMapping
    @Operation(summary = "Add payment method", description = "Save a new payment method for a user")
    public ResponseEntity<PaymentMethodResponse> addMethod(
            @Valid @RequestBody PaymentMethodRequest request,
            HttpServletRequest httpRequest) {
        // Override userId from gateway-injected header
        Long requestingUserId = extractUserId(httpRequest);
        PaymentMethod method = new PaymentMethod();
        method.setUserId(requestingUserId);
        method.setProvider(request.getProvider());
        method.setGatewayToken(request.getGatewayToken());
        method.setLastFour(request.getLastFour());
        method.setCardType(request.getCardType());
        method.setDefault(request.isDefault());
        return ResponseEntity.ok(toResponse(repository.save(method)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> removeMethod(@PathVariable Long id) {
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private void requireOwnerOrAdmin(HttpServletRequest request, Long resourceUserId) {
        String role = request.getHeader("X-User-Role");
        if ("ADMIN".equals(role)) return;
        String requestingUserId = request.getHeader("X-User-Id");
        if (requestingUserId == null || !requestingUserId.equals(String.valueOf(resourceUserId))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }
    }

    private Long extractUserId(HttpServletRequest request) {
        String userId = request.getHeader("X-User-Id");
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing user identity");
        }
        return Long.parseLong(userId);
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
