package com.sarajevotransit.userservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private static final String BLACKLIST_PREFIX = "jti:blacklist:";
    private static final String USER_LOGOUT_ALL_KEY = "user:logout-all:";

    private final StringRedisTemplate redisTemplate;

    public void blacklistJti(String jti, Instant tokenExpiry) {
        long ttlSeconds = Duration.between(Instant.now(), tokenExpiry).getSeconds();
        if (ttlSeconds > 0) {
            redisTemplate.opsForValue().set(BLACKLIST_PREFIX + jti, "1", Duration.ofSeconds(ttlSeconds));
        }
    }

    public void recordLogoutAll(Long userId) {
        redisTemplate.opsForValue().set(
                USER_LOGOUT_ALL_KEY + userId,
                String.valueOf(Instant.now().getEpochSecond()),
                Duration.ofSeconds(900));
    }

    public boolean isBlacklisted(String jti) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_PREFIX + jti));
    }

    public long getLogoutAllTimestamp(Long userId) {
        String value = redisTemplate.opsForValue().get(USER_LOGOUT_ALL_KEY + userId);
        return value == null ? 0L : Long.parseLong(value);
    }
}
