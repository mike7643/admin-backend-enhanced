package com.comprehensive.eureka.admin.integration;

import com.comprehensive.eureka.admin.client.UserStatusClient;
import com.comprehensive.eureka.admin.dto.request.UpdateUserStatusRequestDto;
import com.comprehensive.eureka.admin.dto.request.UserForbiddenWordsChatCreateRequestDto;
import com.comprehensive.eureka.admin.entity.ForbiddenWord;
import com.comprehensive.eureka.admin.entity.UserForbiddenWordsChat;
import com.comprehensive.eureka.admin.enums.Status;
import com.comprehensive.eureka.admin.exception.AdminException;
import com.comprehensive.eureka.admin.repository.ForbiddenWordRepository;
import com.comprehensive.eureka.admin.repository.UserForbiddenWordsChatRepository;
import com.comprehensive.eureka.admin.service.UserForbiddenWordsChatService;
import com.comprehensive.eureka.admin.support.IntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.IntFunction;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * 금칙어 위반 기록의 동시 처리 검증.
 * 같은 사용자의 요청이 동시에 들어와도 누적 건수 기준 제재·해제가 정확히 한 번만 일어나야 한다.
 */
class ForbiddenWordViolationConcurrencyTest extends IntegrationTestSupport {

    private static final Long USER_ID = 7L;
    private static final String WORD = "욕설";
    private static final int THREADS = 10;

    @MockitoBean
    private UserStatusClient userStatusClient;

    @Autowired private UserForbiddenWordsChatService service;
    @Autowired private UserForbiddenWordsChatRepository chatRepository;
    @Autowired private ForbiddenWordRepository forbiddenWordRepository;
    @Autowired private StringRedisTemplate redisTemplate;

    private ForbiddenWord forbiddenWord;

    @BeforeEach
    void setUp() {
        chatRepository.deleteAll();
        forbiddenWordRepository.deleteAll();
        redisTemplate.delete(redisTemplate.keys("forbidden:idempotency:*"));
        reset(userStatusClient);
        forbiddenWord = forbiddenWordRepository.save(ForbiddenWord.builder().word(WORD).status(true).build());
    }

    @Test
    @DisplayName("누적 29건에서 위반 10건이 동시에 들어오면 30건 임계치 제재는 한 번만 호출된다")
    void banIsAppliedExactlyOnceUnderConcurrentViolations() throws Exception {
        seedViolations(29);

        List<Throwable> errors = runConcurrently(THREADS, i -> () -> service.registersUserBadWordsChat(violation(UUID.randomUUID().toString())));

        assertThat(errors).isEmpty();
        assertThat(chatRepository.countByUserId(USER_ID)).isEqualTo(29 + THREADS);
        verify(userStatusClient, times(1)).updateStatus(argThat(req -> req.getStatus() == Status.INACTIVE));
    }

    @Test
    @DisplayName("같은 eventId가 동시에 재전송되어도 위반 기록은 한 번만 저장된다")
    void duplicatedEventIsStoredOnce() throws Exception {
        String eventId = UUID.randomUUID().toString();

        List<Throwable> errors = runConcurrently(5, i -> () -> service.registersUserBadWordsChat(violation(eventId)));

        assertThat(errors).isEmpty();
        assertThat(chatRepository.countByUserId(USER_ID)).isEqualTo(1);
    }

    @Test
    @DisplayName("제재 API가 실패하면 기록이 롤백되고, 같은 eventId 재시도로 정상 처리된다")
    void failedBanRollsBackAndCanBeRetried() {
        seedViolations(29);
        String eventId = UUID.randomUUID().toString();
        willThrow(new IllegalStateException("user service down"))
                .willDoNothing()
                .given(userStatusClient).updateStatus(any());

        assertThatThrownBy(() -> service.registersUserBadWordsChat(violation(eventId)))
                .isInstanceOf(AdminException.class);
        assertThat(chatRepository.countByUserId(USER_ID)).isEqualTo(29);

        service.registersUserBadWordsChat(violation(eventId));

        assertThat(chatRepository.countByUserId(USER_ID)).isEqualTo(30);
        verify(userStatusClient, times(2)).updateStatus(any(UpdateUserStatusRequestDto.class));
    }

    @Test
    @DisplayName("누적 30건에서 기록 2건이 동시에 삭제되면 해제는 한 번만 호출된다")
    void unbanIsAppliedExactlyOnceUnderConcurrentDeletes() throws Exception {
        List<Long> ids = seedViolations(30);
        willDoNothing().given(userStatusClient).updateStatus(any());

        List<Throwable> errors = runConcurrently(2, i -> () -> service.deleteAndProcess(ids.get(i)));

        assertThat(errors).isEmpty();
        assertThat(chatRepository.countByUserId(USER_ID)).isEqualTo(28);
        verify(userStatusClient, times(1)).updateStatus(argThat(req -> req.getStatus() == Status.ACTIVE));
    }

    @Test
    @DisplayName("등록되지 않은 금칙어는 제외하고 나머지만 기록한다")
    void unknownWordIsSkipped() {
        service.registersUserBadWordsChat(UserForbiddenWordsChatCreateRequestDto.builder()
                .userId(USER_ID)
                .chatMessageText("메시지")
                .sentAt(1L)
                .forbiddenWords(List.of(WORD, "삭제된단어"))
                .eventId(UUID.randomUUID().toString())
                .build());

        assertThat(chatRepository.countByUserId(USER_ID)).isEqualTo(1);
    }

    private UserForbiddenWordsChatCreateRequestDto violation(String eventId) {
        return UserForbiddenWordsChatCreateRequestDto.builder()
                .userId(USER_ID)
                .chatMessageText("금칙어가 포함된 메시지")
                .sentAt(System.currentTimeMillis())
                .forbiddenWords(List.of(WORD))
                .eventId(eventId)
                .build();
    }

    private List<Long> seedViolations(int count) {
        List<UserForbiddenWordsChat> logs = IntStream.range(0, count)
                .mapToObj(i -> UserForbiddenWordsChat.builder()
                        .userId(USER_ID)
                        .chatMessageText("seed-" + i)
                        .chatSentAt((long) i)
                        .forbiddenWord(forbiddenWord)
                        .build())
                .toList();
        return chatRepository.saveAll(logs).stream().map(UserForbiddenWordsChat::getId).toList();
    }

    private List<Throwable> runConcurrently(int threads, IntFunction<Runnable> taskFactory) throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        List<Throwable> errors = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < threads; i++) {
            Runnable task = taskFactory.apply(i);
            executor.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    task.run();
                } catch (Throwable t) {
                    errors.add(t);
                } finally {
                    done.countDown();
                }
            });
        }
        ready.await();
        start.countDown();
        assertThat(done.await(30, TimeUnit.SECONDS)).isTrue();
        executor.shutdown();
        return errors;
    }
}
