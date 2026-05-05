package ba.unsa.etf.pnwt.notificationservice.controller;

import ba.unsa.etf.pnwt.notificationservice.dto.*;
import ba.unsa.etf.pnwt.notificationservice.service.NotificationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ResponseEntity<PagedResponse<NotificationResponse>> getAll(
            @PageableDefault(size = 20, sort = "sentAt", direction = Sort.Direction.DESC) Pageable pageable,
            HttpServletRequest httpRequest) {
        requireAdmin(httpRequest);
        return ResponseEntity.ok(notificationService.getAll(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<NotificationResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(notificationService.getById(id));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<PagedResponse<NotificationResponse>> getByUserId(
            @PathVariable Long userId,
            @PageableDefault(size = 20, sort = "sentAt", direction = Sort.Direction.DESC) Pageable pageable,
            HttpServletRequest httpRequest) {
        requireOwnerOrAdmin(httpRequest, userId);
        return ResponseEntity.ok(notificationService.getByUserId(userId, pageable));
    }

    @GetMapping("/user/{userId}/unread")
    public ResponseEntity<List<NotificationResponse>> getUnread(
            @PathVariable Long userId,
            HttpServletRequest httpRequest) {
        requireOwnerOrAdmin(httpRequest, userId);
        return ResponseEntity.ok(notificationService.getUnreadByUserId(userId));
    }

    @GetMapping("/user/{userId}/unread/count")
    public ResponseEntity<Map<String, Long>> countUnread(
            @PathVariable Long userId,
            HttpServletRequest httpRequest) {
        requireOwnerOrAdmin(httpRequest, userId);
        return ResponseEntity.ok(Map.of("count", notificationService.countUnreadByUserId(userId)));
    }

    @GetMapping("/user/{userId}/range")
    public ResponseEntity<List<NotificationResponse>> getByDateRange(
            @PathVariable Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            HttpServletRequest httpRequest) {
        requireOwnerOrAdmin(httpRequest, userId);
        return ResponseEntity.ok(notificationService.getByUserIdAndDateRange(userId, from, to));
    }

    @PostMapping
    public ResponseEntity<NotificationResponse> create(
            @Valid @RequestBody CreateNotificationRequest request,
            HttpServletRequest httpRequest) {
        // Override userId from gateway header — only ADMIN can create notifications for a user
        requireAdmin(httpRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(notificationService.create(request));
    }

    @PostMapping("/batch")
    public ResponseEntity<List<NotificationResponse>> createBatch(
            @Valid @RequestBody BatchCreateNotificationRequest request,
            HttpServletRequest httpRequest) {
        requireAdmin(httpRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(notificationService.createBatch(request));
    }

    @PostMapping("/broadcast")
    public ResponseEntity<BroadcastNotificationResponse> broadcast(
            @Valid @RequestBody BroadcastNotificationRequest request,
            HttpServletRequest httpRequest) {
        requireAdmin(httpRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(notificationService.broadcast(request));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<NotificationResponse> markAsRead(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {
        NotificationResponse notification = notificationService.getById(id);
        requireOwnerOrAdmin(httpRequest, notification.getUserId());
        return ResponseEntity.ok(notificationService.markAsRead(id));
    }

    @PatchMapping("/user/{userId}/read-all")
    public ResponseEntity<Void> markAllAsRead(
            @PathVariable Long userId,
            HttpServletRequest httpRequest) {
        requireOwnerOrAdmin(httpRequest, userId);
        notificationService.markAllAsRead(userId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {
        requireAdmin(httpRequest);
        notificationService.delete(id);
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
}
