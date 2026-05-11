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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RegisterForbiddenWordsChatUseCase {

    private final UserForbiddenWordsChatRepository chatRepository;
    private final ForbiddenWordRepository fwRepository;
    private final UserLockService userLockService;
    private final EventIdempotencyService eventIdempotencyService;
    private final UserModerationService userModerationService;

    public void execute(UserForbiddenWordsChatCreateRequestDto request) {
        Long userId = request.getUserId();
        userLockService.executeWithUserLock(userId, () -> {
            if (eventIdempotencyService.isDuplicateEvent(request.getEventId())) {
                log.info("중복 이벤트 감지로 처리 스킵: eventId={}", request.getEventId());
                return;
            }
            long beforeCount = chatRepository.countByUserId(userId);

            String messageText = request.getChatMessageText();
            Long sentAt = request.getSentAt();
            List<String> forbiddenWords = request.getForbiddenWords() == null
                    ? Collections.emptyList()
                    : request.getForbiddenWords();

            List<UserForbiddenWordsChat> entities = new ArrayList<>();
            for (String word : forbiddenWords) {
                Long fwId = fwRepository.findIdByWord(word)
                        .orElseThrow(() -> new AdminException(ErrorCode.FORBIDDEN_WORD_NOT_FOUND));

                entities.add(UserForbiddenWordsChat.builder()
                        .userId(userId)
                        .chatMessageText(messageText)
                        .chatSentAt(sentAt)
                        .forbiddenWord(fwRepository.getReferenceById(fwId))
                        .build());
            }

            if (entities.isEmpty()) return;

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
            eventIdempotencyService.markProcessed(request.getEventId());
        });
    }
}
