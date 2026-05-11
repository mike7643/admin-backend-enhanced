package com.comprehensive.eureka.admin.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class EventIdempotencyService {

    private static final Duration IDEMPOTENCY_TTL = Duration.ofHours(24);
    private static final String KEY_PREFIX = "forbidden:idempotency:event:";

    private final StringRedisTemplate redisTemplate;

    public boolean isDuplicateEvent(String eventId) {
        if (eventId == null || eventId.isBlank()) {
            return false;
        }
        return redisTemplate.hasKey(KEY_PREFIX + eventId);
    }

    public void markProcessed(String eventId) {
        if (eventId == null || eventId.isBlank()) {
            return;
        }
        redisTemplate.opsForValue().set(KEY_PREFIX + eventId, "1", IDEMPOTENCY_TTL);
    }
}
