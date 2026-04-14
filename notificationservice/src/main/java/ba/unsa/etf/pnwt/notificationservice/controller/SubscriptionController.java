package ba.unsa.etf.pnwt.notificationservice.controller;

import ba.unsa.etf.pnwt.notificationservice.dto.CreateSubscriptionRequest;
import ba.unsa.etf.pnwt.notificationservice.dto.SubscriptionResponse;
import ba.unsa.etf.pnwt.notificationservice.dto.UpdateSubscriptionRequest;
import ba.unsa.etf.pnwt.notificationservice.service.SubscriptionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    public ResponseEntity<SubscriptionResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(subscriptionService.getById(id));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<SubscriptionResponse>> getByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(subscriptionService.getByUserId(userId));
    }

    @GetMapping("/line/{lineId}")
    public ResponseEntity<List<SubscriptionResponse>> getByLineId(@PathVariable Long lineId) {
        return ResponseEntity.ok(subscriptionService.getByLineId(lineId));
    }

    @GetMapping("/user/{userId}/active")
    public ResponseEntity<List<SubscriptionResponse>> getActive(@PathVariable Long userId) {
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
    public ResponseEntity<SubscriptionResponse> deactivate(@PathVariable Long id) {
        return ResponseEntity.ok(subscriptionService.deactivate(id));
    }

    @PatchMapping("/{id}/activate")
    public ResponseEntity<SubscriptionResponse> activate(@PathVariable Long id) {
        return ResponseEntity.ok(subscriptionService.activate(id));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<SubscriptionResponse> update(@PathVariable Long id,
                                                       @Valid @RequestBody UpdateSubscriptionRequest request) {
        return ResponseEntity.ok(subscriptionService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        subscriptionService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
