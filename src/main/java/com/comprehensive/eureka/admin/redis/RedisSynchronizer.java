package com.comprehensive.eureka.admin.redis;

import com.comprehensive.eureka.admin.entity.AllowWord;
import com.comprehensive.eureka.admin.repository.AllowWordRepository;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;


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
        Set<String> forbiddenFromDb = toWordSet(repository.findByStatus(true).stream().map(ForbiddenWord::getWord));
        Set<String> allowFromDb = toWordSet(allowWordRepository.findByStatus(true).stream().map(AllowWord::getWord));

        syncSet(KEY, forbiddenFromDb, words -> toWordSet(
                repository.findByStatusAndWordIn(true, words).stream().map(ForbiddenWord::getWord)));
        syncSet(ALLOW_KEY, allowFromDb, words -> toWordSet(
                allowWordRepository.findByStatusAndWordIn(true, words).stream().map(AllowWord::getWord)));
    }

    /**
     * @param snapshot     동기화 시작 시점의 DB 활성 단어
     * @param activeLookup 반영 직전 DB에서 현재 활성 상태인 단어를 다시 조회하는 함수
     *
     * 스냅샷 이후 커밋된 변경은 리스너가 이미 Redis에 반영했을 수 있다.
     * 오래된 스냅샷만 보고 지우거나 추가하면 그 변경을 되돌리게 되므로, 반영 직전에 DB를 다시 확인한다.
     */
    private void syncSet(String key, Set<String> snapshot, Function<Set<String>, Set<String>> activeLookup) {
        Set<String> current = redisTemplate.opsForSet().members(key);
        if (current == null) {
            current = Set.of();
        }

        // Redis에는 있지만 DB에는 없는 값 제거 (그 사이 다시 활성화된 단어는 제외)
        Set<String> toRemove = new HashSet<>(current);
        toRemove.removeAll(snapshot);
        if (!toRemove.isEmpty()) {
            toRemove.removeAll(activeLookup.apply(toRemove));
        }
        if (!toRemove.isEmpty()) {
            redisTemplate.opsForSet().remove(key, toRemove.toArray());
        }

        // DB에는 있지만 Redis에는 없는 값 추가 (그 사이 삭제·비활성화된 단어는 제외)
        Set<String> toAdd = new HashSet<>(snapshot);
        toAdd.removeAll(current);
        if (!toAdd.isEmpty()) {
            toAdd.retainAll(activeLookup.apply(toAdd));
        }
        if (!toAdd.isEmpty()) {
            redisTemplate.opsForSet().add(key, toAdd.toArray(new String[0]));
        }
    }

    private Set<String> toWordSet(Stream<String> words) {
        return words.collect(Collectors.toCollection(HashSet::new));
    }
}
