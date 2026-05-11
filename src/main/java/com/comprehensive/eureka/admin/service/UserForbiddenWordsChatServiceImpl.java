package com.comprehensive.eureka.admin.service;

import com.comprehensive.eureka.admin.dto.UserForbiddenWordsChatDetailDto;
import com.comprehensive.eureka.admin.dto.request.UserForbiddenWordsChatCreateRequestDto;
import com.comprehensive.eureka.admin.dto.request.UserForbiddenWordsRequestDto;
import com.comprehensive.eureka.admin.entity.UserForbiddenWordsChat;
import com.comprehensive.eureka.admin.exception.AdminException;
import com.comprehensive.eureka.admin.exception.ErrorCode;
import com.comprehensive.eureka.admin.repository.UserForbiddenWordsChatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserForbiddenWordsChatServiceImpl implements UserForbiddenWordsChatService {

    private final UserForbiddenWordsChatRepository chatRepository;
    private final RegisterForbiddenWordsChatUseCase registerForbiddenWordsChatUseCase;
    private final DeleteForbiddenWordsChatUseCase deleteForbiddenWordsChatUseCase;

    @Override
    public List<UserForbiddenWordsChatDetailDto> findDetailByUserId(UserForbiddenWordsRequestDto request) {
        List<UserForbiddenWordsChat> logs;
        Long userId = request.getUserId();
        try {
            logs = chatRepository.findByUserIdOrderByChatSentAtDesc(userId);
        } catch (Exception ex) {
            log.error("금칙어 채팅 기록 조회 실패 (userId={})", userId, ex);
            throw new AdminException(ErrorCode.USER_FORBIDDEN_WORDS_CHAT_RETRIEVE_FAILED);
        }

        if (logs.isEmpty()) {
            return Collections.emptyList();
        }
        return logs.stream()
                .map(log -> new UserForbiddenWordsChatDetailDto(
                        log.getId(),
                        log.getUserId(),
                        log.getForbiddenWord().getWord(),
                        log.getChatMessageText(),
                        log.getChatSentAt()
                ))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void registersUserBadWordsChat(UserForbiddenWordsChatCreateRequestDto request) {
        registerForbiddenWordsChatUseCase.execute(request);
    }

    @Override
    public long countByUserId(Long userId) {
        try {
            return chatRepository.countByUserId(userId);
        } catch (Exception ex) {
            log.error("금칙어 로그 집계 실패, userId={}", userId, ex);
            throw new AdminException(ErrorCode.USER_FORBIDDEN_WORDS_CHAT_AGGREGATE_FAILED);
        }
    }

    @Override
    @Transactional
    public void deleteAndProcess(Long chatLogId) {
        deleteForbiddenWordsChatUseCase.execute(chatLogId);
    }
}
