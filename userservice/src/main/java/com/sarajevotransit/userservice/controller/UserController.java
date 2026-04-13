package com.sarajevotransit.userservice.controller;

import com.sarajevotransit.userservice.dto.AddTicketPurchaseRequest;
import com.sarajevotransit.userservice.dto.AddTravelHistoryRequest;
import com.sarajevotransit.userservice.dto.CreateUserRequest;
import com.sarajevotransit.userservice.dto.TicketPurchaseResponse;
import com.sarajevotransit.userservice.dto.TravelHistoryResponse;
import com.sarajevotransit.userservice.dto.UpdatePasswordRequest;
import com.sarajevotransit.userservice.dto.UpdateUserPreferenceRequest;
import com.sarajevotransit.userservice.dto.UpdateUserProfileRequest;
import com.sarajevotransit.userservice.dto.UserPreferenceResponse;
import com.sarajevotransit.userservice.dto.UserProfileResponse;
import com.sarajevotransit.userservice.dto.UserSummaryResponse;
import com.sarajevotransit.userservice.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@Validated
@RequestMapping({ "/api/users", "/api/v1/users" })
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<UserProfileResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        UserProfileResponse created = userService.createUser(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{userId}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping
    public Page<UserProfileResponse> getAllUsers(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {
        return userService.getAllUsers(page, size, sort);
    }

    @GetMapping("/{userId}")
    public UserProfileResponse getUser(@PathVariable @Positive Long userId) {
        return userService.getUser(userId);
    }

    @GetMapping("/{userId}/preferences")
    public UserPreferenceResponse getPreference(@PathVariable @Positive Long userId) {
        return userService.getPreference(userId);
    }

    @GetMapping("/{userId}/travel-history")
    public Page<TravelHistoryResponse> getTravelHistory(
            @PathVariable @Positive Long userId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "traveledAt,desc") String sort) {
        return userService.getTravelHistory(userId, page, size, sort);
    }

    @GetMapping("/{userId}/ticket-purchases")
    public Page<TicketPurchaseResponse> getTicketPurchases(
            @PathVariable @Positive Long userId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "purchasedAt,desc") String sort) {
        return userService.getTicketPurchases(userId, page, size, sort);
    }

    @PutMapping("/{userId}")
    public UserProfileResponse updateUserProfile(
            @PathVariable @Positive Long userId,
            @Valid @RequestBody UpdateUserProfileRequest request) {
        return userService.updateUserProfile(userId, request);
    }

    @PutMapping("/{userId}/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updatePassword(
            @PathVariable @Positive Long userId,
            @Valid @RequestBody UpdatePasswordRequest request) {
        userService.updatePassword(userId, request);
    }

    @PutMapping("/{userId}/preferences")
    public UserPreferenceResponse updatePreference(
            @PathVariable @Positive Long userId,
            @Valid @RequestBody UpdateUserPreferenceRequest request) {
        return userService.updatePreference(userId, request);
    }

    @PostMapping("/{userId}/travel-history")
    public ResponseEntity<TravelHistoryResponse> addTravelHistory(
            @PathVariable @Positive Long userId,
            @Valid @RequestBody AddTravelHistoryRequest request) {
        TravelHistoryResponse created = userService.addTravelHistory(userId, request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{entryId}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PostMapping("/{userId}/ticket-purchases")
    public ResponseEntity<TicketPurchaseResponse> addTicketPurchase(
            @PathVariable @Positive Long userId,
            @Valid @RequestBody AddTicketPurchaseRequest request) {
        TicketPurchaseResponse created = userService.addTicketPurchase(userId, request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{purchaseId}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping("/{userId}/summary")
    public UserSummaryResponse getSummary(@PathVariable @Positive Long userId) {
        return userService.getUserSummary(userId);
    }

    @GetMapping("/{userId}/suggestions")
    public List<String> getSuggestions(
            @PathVariable @Positive Long userId,
            @RequestParam(defaultValue = "3") @Min(1) @Max(10) int limit) {
        return userService.getPersonalizedLineSuggestions(userId, limit);
    }
}
