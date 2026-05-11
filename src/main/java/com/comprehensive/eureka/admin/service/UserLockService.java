package com.comprehensive.eureka.admin.service;

import com.comprehensive.eureka.admin.exception.AdminException;
import com.comprehensive.eureka.admin.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class UserLockService {

    private static final Duration LOCK_TTL = Duration.ofSeconds(10);
    private static final Duration LOCK_WAIT = Duration.ofSeconds(2);
    private static final String LOCK_KEY_PREFIX = "forbidden:lock:user:";

    private final RedissonClient redissonClient;

    public void executeWithUserLock(Long userId, Runnable action) {
        RLock lock = redissonClient.getLock(LOCK_KEY_PREFIX + userId);
        boolean acquired;
        try {
            acquired = lock.tryLock(LOCK_WAIT.toMillis(), LOCK_TTL.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AdminException(ErrorCode.USER_FORBIDDEN_WORDS_CHAT_LOCK_FAILED);
        }
        if (!acquired) {
            throw new AdminException(ErrorCode.USER_FORBIDDEN_WORDS_CHAT_LOCK_FAILED);
        }

        try {
            action.run();
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
