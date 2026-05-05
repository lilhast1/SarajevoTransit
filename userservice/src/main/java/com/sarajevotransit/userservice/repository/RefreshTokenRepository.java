package com.sarajevotransit.userservice.repository;

import com.sarajevotransit.userservice.model.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    void deleteByToken(String token);

    void deleteByUserId(Long userId);

    void deleteAllByExpiresAtBefore(LocalDateTime cutoff);
}
