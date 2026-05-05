package ba.unsa.etf.pnwt.notificationservice.controller;

import ba.unsa.etf.pnwt.notificationservice.dto.CreateSubscriptionRequest;
import ba.unsa.etf.pnwt.notificationservice.dto.PagedResponse;
import ba.unsa.etf.pnwt.notificationservice.dto.SubscriptionResponse;
import ba.unsa.etf.pnwt.notificationservice.dto.UpdateSubscriptionRequest;
import ba.unsa.etf.pnwt.notificationservice.service.SubscriptionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

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
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            HttpServletRequest httpRequest) {
        requireAdmin(httpRequest);
        return ResponseEntity.ok(subscriptionService.getAll(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SubscriptionResponse> getById(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {
        SubscriptionResponse sub = subscriptionService.getById(id);
        requireOwnerOrAdmin(httpRequest, sub.getUserId());
        return ResponseEntity.ok(sub);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<SubscriptionResponse>> getByUserId(
            @PathVariable Long userId,
            HttpServletRequest httpRequest) {
        requireOwnerOrAdmin(httpRequest, userId);
        return ResponseEntity.ok(subscriptionService.getByUserId(userId));
    }

    @GetMapping("/line/{lineId}")
    public ResponseEntity<PagedResponse<SubscriptionResponse>> getByLineId(
            @PathVariable Long lineId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(subscriptionService.getByLineId(lineId, pageable));
    }

    @GetMapping("/user/{userId}/active")
    public ResponseEntity<List<SubscriptionResponse>> getActive(
            @PathVariable Long userId,
            HttpServletRequest httpRequest) {
        requireOwnerOrAdmin(httpRequest, userId);
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
            @RequestParam(required = false) String email,
            HttpServletRequest httpRequest) {
        requireAdmin(httpRequest);
        if (name != null) return ResponseEntity.ok(subscriptionService.searchByName(name));
        if (email != null) return ResponseEntity.ok(subscriptionService.searchByEmail(email));
        return ResponseEntity.ok(List.of());
    }

    @PostMapping
    public ResponseEntity<SubscriptionResponse> create(
            @Valid @RequestBody CreateSubscriptionRequest request,
            HttpServletRequest httpRequest) {
        // Override userId from gateway header so passengers can't subscribe on behalf of others
        Long requestingUserId = extractUserId(httpRequest);
        request.setUserId(requestingUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(subscriptionService.create(request));
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<SubscriptionResponse> deactivate(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {
        SubscriptionResponse sub = subscriptionService.getById(id);
        requireOwnerOrAdmin(httpRequest, sub.getUserId());
        return ResponseEntity.ok(subscriptionService.deactivate(id));
    }

    @PatchMapping("/{id}/activate")
    public ResponseEntity<SubscriptionResponse> activate(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {
        SubscriptionResponse sub = subscriptionService.getById(id);
        requireOwnerOrAdmin(httpRequest, sub.getUserId());
        return ResponseEntity.ok(subscriptionService.activate(id));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<SubscriptionResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateSubscriptionRequest request,
            HttpServletRequest httpRequest) {
        SubscriptionResponse sub = subscriptionService.getById(id);
        requireOwnerOrAdmin(httpRequest, sub.getUserId());
        return ResponseEntity.ok(subscriptionService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {
        SubscriptionResponse sub = subscriptionService.getById(id);
        requireOwnerOrAdmin(httpRequest, sub.getUserId());
        subscriptionService.delete(id);
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

    private void requireAdmin(HttpServletRequest request) {
        if (!"ADMIN".equals(request.getHeader("X-User-Role"))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin access required");
        }
    }

    private Long extractUserId(HttpServletRequest request) {
        String userId = request.getHeader("X-User-Id");
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing user identity");
        }
        return Long.parseLong(userId);
    }
}
