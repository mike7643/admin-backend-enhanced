package com.comprehensive.eureka.admin.service;

import com.comprehensive.eureka.admin.dto.request.UserForbiddenWordsChatCreateRequestDto;
import com.comprehensive.eureka.admin.entity.UserForbiddenWordsChat;
import com.comprehensive.eureka.admin.exception.AdminException;
import com.comprehensive.eureka.admin.exception.ErrorCode;
import com.comprehensive.eureka.admin.repository.ForbiddenWordRepository;
import com.comprehensive.eureka.admin.repository.UserForbiddenWordsChatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * 금칙어 위반 기록 저장 → 누적 건수 재집계 → 임계치 판단을 사용자 단위로 직렬화한다.
 *
 * 처리 순서: 락 획득 → 트랜잭션 시작 → 저장·집계·제재 → 커밋 → 멱등성 마킹 → 락 해제
 * 락이 커밋보다 먼저 풀리면 다음 요청이 아직 커밋되지 않은 건수를 보지 못해 같은 임계치를 다시 넘었다고 판단할 수 있다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RegisterForbiddenWordsChatUseCase {

    private final UserForbiddenWordsChatRepository chatRepository;
    private final ForbiddenWordRepository fwRepository;
    private final UserLockService userLockService;
    private final EventIdempotencyService eventIdempotencyService;
    private final UserModerationService userModerationService;
    private final TransactionTemplate transactionTemplate;

    public void execute(UserForbiddenWordsChatCreateRequestDto request) {
        Long userId = request.getUserId();
        userLockService.executeWithUserLock(userId, () -> {
            if (eventIdempotencyService.isDuplicateEvent(request.getEventId())) {
                log.info("중복 이벤트 감지로 처리 스킵: eventId={}", request.getEventId());
                return;
            }

            // 제재 API 실패 시 위반 기록도 함께 롤백되어, 호출 측이 같은 eventId로 재시도할 수 있다.
            transactionTemplate.executeWithoutResult(status -> saveAndModerate(userId, request));

            // 커밋 이후에만 처리 완료로 기록해야 롤백된 이벤트가 중복으로 오인되지 않는다.
            eventIdempotencyService.markProcessed(request.getEventId());
        });
    }

    private void saveAndModerate(Long userId, UserForbiddenWordsChatCreateRequestDto request) {
        List<UserForbiddenWordsChat> entities = toEntities(userId, request);
        if (entities.isEmpty()) return;

        long beforeCount = chatRepository.countByUserId(userId);

        try {
            chatRepository.saveAll(entities);
        } catch (Exception ex) {
            log.error("금칙어 채팅 기록 저장 실패", ex);
            throw new AdminException(ErrorCode.USER_FORBIDDEN_WORDS_CHAT_SAVE_FAILED);
        }

        long afterCount;
        try {
            afterCount = chatRepository.countByUserId(userId);
        } catch (Exception ex) {
            log.error("금칙어 로그 집계 실패, userId={}", userId, ex);
            throw new AdminException(ErrorCode.USER_FORBIDDEN_WORDS_CHAT_AGGREGATE_FAILED);
        }
        userModerationService.applyBanIfThresholdCrossed(userId, beforeCount, afterCount);
    }

    private List<UserForbiddenWordsChat> toEntities(Long userId, UserForbiddenWordsChatCreateRequestDto request) {
        List<String> forbiddenWords = request.getForbiddenWords() == null
                ? Collections.emptyList()
                : request.getForbiddenWords();

        List<UserForbiddenWordsChat> entities = new ArrayList<>();
        for (String word : forbiddenWords) {
            Optional<Long> fwId = fwRepository.findIdByWord(word);
            if (fwId.isEmpty()) {
                // 챗봇이 감지한 직후 관리자가 단어를 삭제한 경우: 이벤트 전체를 실패시키면 재시도해도 계속 실패하므로 해당 단어만 제외한다.
                log.warn("등록되지 않은 금칙어는 기록에서 제외: userId={}, word={}", userId, word);
                continue;
            }
            entities.add(UserForbiddenWordsChat.builder()
                    .userId(userId)
                    .chatMessageText(request.getChatMessageText())
                    .chatSentAt(request.getSentAt())
                    .forbiddenWord(fwRepository.getReferenceById(fwId.get()))
                    .build());
        }
        return entities;
    }
}
