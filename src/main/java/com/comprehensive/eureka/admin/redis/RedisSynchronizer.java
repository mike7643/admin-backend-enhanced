package com.comprehensive.eureka.admin.redis;

import com.comprehensive.eureka.admin.entity.AllowWord;
import com.comprehensive.eureka.admin.repository.AllowWordRepository;
import java.util.HashSet;
import java.util.Set;


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
     * DB → Redis 보정 동기화
     * 키를 지웠다가 다시 채우면 그 사이 챗봇이 빈 Set을 읽게 되므로, 차이만 반영한다.
     * 이벤트 리스너가 실패해 Redis에 반영되지 못한 변경도 이 주기에서 보정된다.
     */
    private void syncDbToRedis() {
        // 1) DB에서 status=true인 금칙어/허용어 목록 조회
        Set<String> forbiddenFromDb = new HashSet<>(
                repository.findByStatus(true)
                        .stream()
                        .map(ForbiddenWord::getWord)
                        .toList()
        );

        Set<String> allowFromDb = new HashSet<>(
                allowWordRepository.findByStatus(true)
                        .stream()
                        .map(AllowWord::getWord)
                        .toList()
        );

        // 2) Redis와의 차집합 기준 동기화
        syncSet(KEY, forbiddenFromDb);
        syncSet(ALLOW_KEY, allowFromDb);
    }

    private void syncSet(String key, Set<String> expected) {
        Set<String> current = redisTemplate.opsForSet().members(key);
        if (current == null) {
            current = Set.of();
        }

        // Redis에는 있지만 DB에는 없는 값 제거
        Set<String> toRemove = new HashSet<>(current);
        toRemove.removeAll(expected);
        if (!toRemove.isEmpty()) {
            Object[] removeValues = toRemove.toArray();
            redisTemplate.opsForSet().remove(key, removeValues);
        }

        // DB에는 있지만 Redis에는 없는 값 추가
        Set<String> toAdd = new HashSet<>(expected);
        toAdd.removeAll(current);
        if (!toAdd.isEmpty()) {
            redisTemplate.opsForSet().add(key, toAdd.toArray(new String[0]));
        }
    }
}
