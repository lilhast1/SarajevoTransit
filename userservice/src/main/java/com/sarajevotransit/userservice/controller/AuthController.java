package com.sarajevotransit.userservice.controller;

import com.sarajevotransit.userservice.dto.LoginRequest;
import com.sarajevotransit.userservice.dto.LoginResponse;
import com.sarajevotransit.userservice.dto.LogoutAllRequest;
import com.sarajevotransit.userservice.dto.LogoutRequest;
import com.sarajevotransit.userservice.dto.RefreshTokenRequest;
import com.sarajevotransit.userservice.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refresh(request.refreshToken()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody LogoutRequest request) {
        authService.logout(request.refreshToken(), request.accessToken());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/logout-all")
    public ResponseEntity<Void> logoutAll(@Valid @RequestBody LogoutAllRequest request) {
        authService.logoutAll(request.accessToken());
        return ResponseEntity.noContent().build();
    }
}
