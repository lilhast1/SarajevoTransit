package ba.unsa.etf.pnwt.notificationservice.controller;

import ba.unsa.etf.pnwt.notificationservice.dto.CreateSubscriptionRequest;
import ba.unsa.etf.pnwt.notificationservice.dto.PagedResponse;
import ba.unsa.etf.pnwt.notificationservice.dto.SubscriptionResponse;
import ba.unsa.etf.pnwt.notificationservice.dto.UpdateSubscriptionRequest;
import ba.unsa.etf.pnwt.notificationservice.service.SubscriptionService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/subscriptions")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    public SubscriptionController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @GetMapping
    public ResponseEntity<PagedResponse<SubscriptionResponse>> getAll(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(subscriptionService.getAll(pageable));
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
    public ResponseEntity<PagedResponse<SubscriptionResponse>> getByLineId(
            @PathVariable Long lineId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(subscriptionService.getByLineId(lineId, pageable));
    }

    @GetMapping("/user/{userId}/active")
    public ResponseEntity<List<SubscriptionResponse>> getActive(@PathVariable Long userId) {
        return ResponseEntity.ok(subscriptionService.getActiveByUserId(userId));
    }

    @GetMapping("/line/{lineId}/active/count")
    public ResponseEntity<Map<String, Long>> countActiveByLine(@PathVariable Long lineId) {
        return ResponseEntity.ok(Map.of("count", subscriptionService.countActiveByLineId(lineId)));
    }

    @GetMapping("/line/{lineId}/active-at")
    public ResponseEntity<List<SubscriptionResponse>> getActiveAtTime(
            @PathVariable Long lineId,
            @RequestParam LocalTime time,
            @RequestParam String day) {
        return ResponseEntity.ok(subscriptionService.getActiveForLineAtTime(lineId, time, day));
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
        return ResponseEntity.ok(List.of());
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
