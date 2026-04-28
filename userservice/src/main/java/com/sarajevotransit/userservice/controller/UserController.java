package com.sarajevotransit.userservice.controller;

import com.sarajevotransit.userservice.dto.AddTicketPurchaseRequest;
import com.sarajevotransit.userservice.dto.AddTravelHistoryRequest;
import com.sarajevotransit.userservice.dto.CreateUserRequest;
import com.sarajevotransit.userservice.dto.PaginationRequest;
import com.sarajevotransit.userservice.dto.TicketPurchaseResponse;
import com.sarajevotransit.userservice.dto.TicketPurchaseStatsResponse;
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
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping({ "/api/users", "/api/v1/users" })
public class UserController {

    private final UserService userService;

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
            @Valid PaginationRequest request) {
        return userService.getAllUsers(request.getPage(), request.getSize(), request.getSort());
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
            @Valid PaginationRequest request) {
        return userService.getTravelHistory(userId, request.getPage(), request.getSize(), request.getSort());
    }

    @GetMapping("/{userId}/ticket-purchases")
    public Page<TicketPurchaseResponse> getTicketPurchases(
            @PathVariable @Positive Long userId,
            @Valid PaginationRequest request) {
        return userService.getTicketPurchases(userId, request.getPage(), request.getSize(), request.getSort());
    }

    @PutMapping("/{userId}")
    public UserProfileResponse updateUserProfile(
            @PathVariable @Positive Long userId,
            @Valid @RequestBody UpdateUserProfileRequest request) {
        return userService.updateUserProfile(userId, request);
    }

    @PatchMapping(path = "/{userId}", consumes = "application/json-patch+json")
    public UserProfileResponse patchUserProfile(
            @PathVariable @Positive Long userId,
            @RequestBody String patchDocument) {
        return userService.patchUserProfile(userId, patchDocument);
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

    @PostMapping("/{userId}/travel-history/batch")
    public ResponseEntity<List<TravelHistoryResponse>> addTravelHistoryBatch(
            @PathVariable @Positive Long userId,
            @RequestBody @Size(min = 1, max = 200, message = "Batch size must be between 1 and 200 entries") List<@Valid AddTravelHistoryRequest> requests) {
        List<TravelHistoryResponse> created = userService.addTravelHistoryBatch(userId, requests);
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/users/{userId}/travel-history")
                .buildAndExpand(userId)
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

    @GetMapping("/{userId}/ticket-purchases/stats")
    public List<TicketPurchaseStatsResponse> getTicketPurchaseStats(
            @PathVariable @Positive Long userId) {
        return userService.getTicketPurchaseStats(userId);
    }

    @DeleteMapping("/{userId}/travel-history/{entryId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTravelHistoryEntry(
            @PathVariable @Positive Long userId,
            @PathVariable @Positive Long entryId) {
        userService.deleteTravelHistoryEntry(userId, entryId);
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
