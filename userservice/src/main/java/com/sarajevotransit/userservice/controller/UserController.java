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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<UserProfileResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(request));
    }

    @GetMapping
    public List<UserProfileResponse> getAllUsers() {
        return userService.getAllUsers();
    }

    @GetMapping("/{userId}")
    public UserProfileResponse getUser(@PathVariable Long userId) {
        return userService.getUser(userId);
    }

    @PutMapping("/{userId}")
    public UserProfileResponse updateUserProfile(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateUserProfileRequest request) {
        return userService.updateUserProfile(userId, request);
    }

    @PutMapping("/{userId}/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updatePassword(
            @PathVariable Long userId,
            @Valid @RequestBody UpdatePasswordRequest request) {
        userService.updatePassword(userId, request);
    }

    @PutMapping("/{userId}/preferences")
    public UserPreferenceResponse updatePreference(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateUserPreferenceRequest request) {
        return userService.updatePreference(userId, request);
    }

    @PostMapping("/{userId}/travel-history")
    public ResponseEntity<TravelHistoryResponse> addTravelHistory(
            @PathVariable Long userId,
            @Valid @RequestBody AddTravelHistoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.addTravelHistory(userId, request));
    }

    @PostMapping("/{userId}/ticket-purchases")
    public ResponseEntity<TicketPurchaseResponse> addTicketPurchase(
            @PathVariable Long userId,
            @Valid @RequestBody AddTicketPurchaseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.addTicketPurchase(userId, request));
    }

    @GetMapping("/{userId}/summary")
    public UserSummaryResponse getSummary(@PathVariable Long userId) {
        return userService.getUserSummary(userId);
    }

    @GetMapping("/{userId}/suggestions")
    public List<String> getSuggestions(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "3") int limit) {
        return userService.getPersonalizedLineSuggestions(userId, limit);
    }
}
