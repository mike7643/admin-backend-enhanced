package com.comprehensive.eureka.admin.service;

import com.comprehensive.eureka.admin.entity.UserForbiddenWordsChat;
import com.comprehensive.eureka.admin.exception.AdminException;
import com.comprehensive.eureka.admin.exception.ErrorCode;
import com.comprehensive.eureka.admin.repository.UserForbiddenWordsChatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeleteForbiddenWordsChatUseCase {

    private final UserForbiddenWordsChatRepository chatRepository;
    private final UserLockService userLockService;
    private final UserModerationService userModerationService;

    public void execute(Long chatLogId) {
        UserForbiddenWordsChat record = chatRepository.findById(chatLogId)
                .orElseThrow(() -> new AdminException(ErrorCode.USER_FORBIDDEN_WORDS_CHAT_RETRIEVE_FAILED));
        Long userId = record.getUserId();

        userLockService.executeWithUserLock(userId, () -> {
            try {
                chatRepository.deleteById(chatLogId);
            } catch (Exception ex) {
                log.error("금칙어 로그 삭제 실패, id={}", chatLogId, ex);
                throw new AdminException(ErrorCode.USER_FORBIDDEN_WORDS_CHAT_DELETE_FAILED);
            }

            long afterCount;
            try {
                afterCount = chatRepository.countByUserId(userId);
            } catch (Exception ex) {
                log.error("위반 횟수 집계 실패, userId={}", userId, ex);
                throw new AdminException(ErrorCode.USER_FORBIDDEN_WORDS_CHAT_AGGREGATE_FAILED);
            }
            long beforeCount = afterCount + 1;
            userModerationService.applyUnbanIfThresholdDropped(userId, beforeCount, afterCount);
        });
    }
}
