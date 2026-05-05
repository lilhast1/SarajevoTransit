package com.sarajevotransit.userservice.service;

import com.sarajevotransit.userservice.dto.LoginRequest;
import com.sarajevotransit.userservice.dto.LoginResponse;
import com.sarajevotransit.userservice.exception.InvalidCredentialsException;
import com.sarajevotransit.userservice.model.RefreshToken;
import com.sarajevotransit.userservice.model.UserProfile;
import com.sarajevotransit.userservice.repository.RefreshTokenRepository;
import com.sarajevotransit.userservice.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserProfileRepository userProfileRepository;

    @Value("${jwt.access-token.expiration}")
    private long accessTokenExpirationSeconds;

    @Value("${jwt.refresh-token.expiration}")
    private long refreshTokenExpirationSeconds;

    @Transactional
    public LoginResponse login(LoginRequest request) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email().trim().toLowerCase(), request.password()));
        } catch (BadCredentialsException e) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        UserProfile user = (UserProfile) authentication.getPrincipal();
        String accessToken = jwtService.generateAccessToken(user);
        String refreshTokenValue = createRefreshToken(user.getId());

        return new LoginResponse(
                accessToken,
                refreshTokenValue,
                "Bearer",
                accessTokenExpirationSeconds,
                user.getId(),
                user.getEmail(),
                user.getRole().name());
    }

    @Transactional
    public LoginResponse refresh(String rawRefreshToken) {
        RefreshToken stored = refreshTokenRepository.findByToken(rawRefreshToken)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid or expired refresh token"));

        if (stored.getExpiresAt().isBefore(LocalDateTime.now())) {
            refreshTokenRepository.delete(stored);
            throw new InvalidCredentialsException("Refresh token has expired");
        }

        UserProfile user = userProfileRepository.findById(stored.getUserId())
                .orElseThrow(() -> new InvalidCredentialsException("User not found"));

        // rotate refresh token
        refreshTokenRepository.delete(stored);
        String newRefreshToken = createRefreshToken(user.getId());
        String newAccessToken = jwtService.generateAccessToken(user);

        return new LoginResponse(
                newAccessToken,
                newRefreshToken,
                "Bearer",
                accessTokenExpirationSeconds,
                user.getId(),
                user.getEmail(),
                user.getRole().name());
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        refreshTokenRepository.deleteByToken(rawRefreshToken);
    }

    private String createRefreshToken(Long userId) {
        RefreshToken token = new RefreshToken();
        token.setUserId(userId);
        token.setToken(UUID.randomUUID().toString().replace("-", ""));
        token.setCreatedAt(LocalDateTime.now());
        token.setExpiresAt(LocalDateTime.now().plusSeconds(refreshTokenExpirationSeconds));
        refreshTokenRepository.save(token);
        return token.getToken();
    }
}
