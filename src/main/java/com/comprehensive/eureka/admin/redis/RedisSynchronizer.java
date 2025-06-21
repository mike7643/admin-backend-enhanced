package com.comprehensive.eureka.admin.redis;

import com.comprehensive.eureka.admin.entity.AllowWord;
import com.comprehensive.eureka.admin.repository.AllowWordRepository;
import com.comprehensive.eureka.admin.service.AllowWordRedisService;
import java.util.List;


import jakarta.annotation.PostConstruct;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.comprehensive.eureka.admin.entity.ForbiddenWord;
import com.comprehensive.eureka.admin.repository.ForbiddenWordRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@EnableScheduling
public class RedisSynchronizer {

    private static final String KEY = "forbidden:words";
    private static final String ALLOW_KEY = "allow:words";

    private final ForbiddenWordRepository repository;
    private final AllowWordRepository allowWordRepository;
    private final StringRedisTemplate redisTemplate;

    /** 
     * 애플리케이션 기동 직후 한 번 동기화
     */
    @PostConstruct
    public void initSync() {
        syncDbToRedis();
    }

    /** 
     * 5분마다 자동 동기화
     */
    @Scheduled(fixedDelay = 5 * 60 * 1000)
    public void periodicSync() {
        syncDbToRedis();
    }

    /**
     * DB → Redis 완전 동기화
     */
    private void syncDbToRedis() {
        // 1) Redis 키 초기화 (삭제)
        redisTemplate.delete(KEY);
        redisTemplate.delete(ALLOW_KEY);

        // 2) DB에서 status=true인 금칙어 word 목록 조회
        List<String> words = repository.findByStatus(true)
                                       .stream()
                                       .map(ForbiddenWord::getWord)
                                       .toList();

        // 3) Redis Set에 다시 적재
        if (!words.isEmpty()) {
            redisTemplate.opsForSet().add(KEY, words.toArray(new String[0]));
        }

        words = allowWordRepository.findByStatus(true)
                                       .stream()
                                       .map(AllowWord::getWord)
                                       .toList();

        if (!words.isEmpty()) {
            redisTemplate.opsForSet().add(ALLOW_KEY, words.toArray(new String[0]));
        }
    }
}
