package ba.unsa.etf.pnwt.notificationservice.controller;

import ba.unsa.etf.pnwt.notificationservice.dto.CreateSubscriptionRequest;
import ba.unsa.etf.pnwt.notificationservice.dto.SubscriptionResponse;
import ba.unsa.etf.pnwt.notificationservice.service.SubscriptionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/subscriptions")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    public SubscriptionController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @GetMapping
    public ResponseEntity<List<SubscriptionResponse>> getAll() {
        return ResponseEntity.ok(subscriptionService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SubscriptionResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(subscriptionService.getById(id));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<SubscriptionResponse>> getByUserId(@PathVariable UUID userId) {
        return ResponseEntity.ok(subscriptionService.getByUserId(userId));
    }

    @GetMapping("/line/{lineId}")
    public ResponseEntity<List<SubscriptionResponse>> getByLineId(@PathVariable UUID lineId) {
        return ResponseEntity.ok(subscriptionService.getByLineId(lineId));
    }

    @GetMapping("/user/{userId}/active")
    public ResponseEntity<List<SubscriptionResponse>> getActive(@PathVariable UUID userId) {
        return ResponseEntity.ok(subscriptionService.getActiveByUserId(userId));
    }

    @GetMapping("/search")
    public ResponseEntity<List<SubscriptionResponse>> search(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String email) {
        if (name != null) {
            return ResponseEntity.ok(subscriptionService.searchByName(name));
        }
        if (email != null) {
            return ResponseEntity.ok(subscriptionService.searchByEmail(email));
        }
        return ResponseEntity.ok(subscriptionService.getAll());
    }

    @PostMapping
    public ResponseEntity<SubscriptionResponse> create(@Valid @RequestBody CreateSubscriptionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(subscriptionService.create(request));
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<SubscriptionResponse> deactivate(@PathVariable UUID id) {
        return ResponseEntity.ok(subscriptionService.deactivate(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        subscriptionService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
