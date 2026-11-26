package com.comprehensive.eureka.admin.integration;

import com.comprehensive.eureka.admin.dto.AllowWordRequestDto;
import com.comprehensive.eureka.admin.dto.request.ForbiddenWordRequestDto;
import com.comprehensive.eureka.admin.dto.response.ForbiddenWordResponseDto;
import com.comprehensive.eureka.admin.entity.ForbiddenWord;
import com.comprehensive.eureka.admin.redis.RedisSynchronizer;
import com.comprehensive.eureka.admin.repository.AllowWordRepository;
import com.comprehensive.eureka.admin.repository.ForbiddenWordRepository;
import com.comprehensive.eureka.admin.service.AllowWordService;
import com.comprehensive.eureka.admin.service.ForbiddenWordService;
import com.comprehensive.eureka.admin.support.IntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WordCacheSyncIntegrationTest extends IntegrationTestSupport {

    private static final String FORBIDDEN_KEY = "forbidden:words";
    private static final String ALLOW_KEY = "allow:words";

    @Autowired private ForbiddenWordService forbiddenWordService;
    @Autowired private AllowWordService allowWordService;
    @Autowired private ForbiddenWordRepository forbiddenWordRepository;
    @Autowired private AllowWordRepository allowWordRepository;
    @Autowired private StringRedisTemplate redisTemplate;
    @Autowired private TransactionTemplate transactionTemplate;
    @Autowired private RedisSynchronizer redisSynchronizer;

    @BeforeEach
    void cleanUp() {
        forbiddenWordRepository.deleteAll();
        allowWordRepository.deleteAll();
        redisTemplate.delete(FORBIDDEN_KEY);
        redisTemplate.delete(ALLOW_KEY);
    }

    @Test
    @DisplayName("금칙어 등록이 커밋되면 Redis Set에 반영된다")
    void forbiddenWordIsSyncedAfterCommit() {
        forbiddenWordService.addForbiddenWord(new ForbiddenWordRequestDto("나쁜말", true));

        assertThat(redisTemplate.opsForSet().isMember(FORBIDDEN_KEY, "나쁜말")).isTrue();
    }

    @Test
    @DisplayName("허용어 등록이 커밋되면 Redis Set에 반영된다")
    void allowWordIsSyncedAfterCommit() {
        allowWordService.addAllowWord(new AllowWordRequestDto("허용말", true));

        assertThat(redisTemplate.opsForSet().isMember(ALLOW_KEY, "허용말")).isTrue();
    }

    @Test
    @DisplayName("트랜잭션이 롤백되면 Redis에 반영되지 않는다")
    void rolledBackChangeIsNotSynced() {
        assertThatThrownBy(() -> transactionTemplate.executeWithoutResult(status -> {
            forbiddenWordService.addForbiddenWord(new ForbiddenWordRequestDto("롤백될말", true));
            throw new IllegalStateException("상위 트랜잭션 실패");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(forbiddenWordRepository.existsByWord("롤백될말")).isFalse();
        assertThat(redisTemplate.opsForSet().isMember(FORBIDDEN_KEY, "롤백될말")).isFalse();
    }

    @Test
    @DisplayName("상태 토글과 삭제도 커밋 이후 Redis에 반영된다")
    void toggleAndDeleteAreSynced() {
        ForbiddenWordResponseDto saved = forbiddenWordService.addForbiddenWord(new ForbiddenWordRequestDto("토글말", true));

        forbiddenWordService.toggleForbiddenWordStatus(saved.getId());
        assertThat(redisTemplate.opsForSet().isMember(FORBIDDEN_KEY, "토글말")).isFalse();

        forbiddenWordService.toggleForbiddenWordStatus(saved.getId());
        assertThat(redisTemplate.opsForSet().isMember(FORBIDDEN_KEY, "토글말")).isTrue();

        forbiddenWordService.deleteForbiddenWord(saved.getId());
        assertThat(redisTemplate.opsForSet().isMember(FORBIDDEN_KEY, "토글말")).isFalse();
    }

    @Test
    @DisplayName("주기 동기화는 키를 비우지 않고 DB와의 차이만 보정한다")
    void periodicSyncReconcilesOnlyTheDifference() {
        // 리스너 실패 등으로 어긋난 상태: DB에만 있는 단어, Redis에만 남은 단어, 양쪽에 모두 있는 단어
        forbiddenWordRepository.save(ForbiddenWord.builder().word("DB에만").status(true).build());
        forbiddenWordRepository.save(ForbiddenWord.builder().word("양쪽모두").status(true).build());
        forbiddenWordRepository.save(ForbiddenWord.builder().word("비활성").status(false).build());
        redisTemplate.opsForSet().add(FORBIDDEN_KEY, "양쪽모두", "Redis에만", "비활성");

        redisSynchronizer.periodicSync();

        assertThat(redisTemplate.opsForSet().members(FORBIDDEN_KEY))
                .containsExactlyInAnyOrder("DB에만", "양쪽모두");
    }
}
