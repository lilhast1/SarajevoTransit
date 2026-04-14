package com.sarajevotransit.moneyman.controller;

import com.sarajevotransit.moneyman.model.PaymentMethod;
import com.sarajevotransit.moneyman.repository.PaymentMethodRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments/methods")
@RequiredArgsConstructor
public class PaymentMethodController {

    private final PaymentMethodRepository repository;

    @GetMapping("/{userId}")
    public List<PaymentMethod> getMethods(@PathVariable Long userId) {
        return repository.findByUserId(userId);
    }

    @PostMapping
    public PaymentMethod addMethod(@RequestBody PaymentMethod method) {
        // F11: In real life, the card info is sent to Stripe, 
        // Stripe returns a token, and we save ONLY that token.
        return repository.save(method);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> removeMethod(@PathVariable Long id) {
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}