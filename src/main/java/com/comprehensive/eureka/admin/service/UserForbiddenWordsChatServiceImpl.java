package com.comprehensive.eureka.admin.service;

import com.comprehensive.eureka.admin.dto.UserForbiddenWordsChatDto;
import com.comprehensive.eureka.admin.dto.request.UpdateUserStatusRequestDto;
import com.comprehensive.eureka.admin.dto.request.UserForbiddenWordsChatCreateRequestDto;
import com.comprehensive.eureka.admin.entity.UserForbiddenWordsChat;
import com.comprehensive.eureka.admin.enums.Status;
import com.comprehensive.eureka.admin.exception.AdminException;
import com.comprehensive.eureka.admin.exception.ErrorCode;
import com.comprehensive.eureka.admin.repository.ForbiddenWordRepository;
import com.comprehensive.eureka.admin.repository.UserForbiddenWordsChatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserForbiddenWordsChatServiceImpl implements UserForbiddenWordsChatService {

    private final UserForbiddenWordsChatRepository chatRepository;
    private final ForbiddenWordRepository fwRepository;


    @Qualifier("userClient")
    private final WebClient userClient;

    /**
     * 특정 사용자 ID로 금칙어 채팅 기록 조회
     */
    @Override
    public List<UserForbiddenWordsChatDto> findByUserId(Long userId) {
        List<UserForbiddenWordsChat> logs;
        try {
            logs = chatRepository.findByUserId(userId);
        } catch (Exception ex) {
            log.error("금칙어 채팅 기록 조회 실패 (userId={})", userId, ex);
            throw new AdminException(ErrorCode.USER_FORBIDDEN_WORDS_CHAT_RETRIEVE_FAILED);
        }

        if (logs == null || logs.isEmpty()) {
            return Collections.emptyList();
        }

        return logs.stream()
                .map(UserForbiddenWordsChatDto::from)
                .collect(Collectors.toList());
    }

    /**
     * 다중 금칙어 ID로 한 건씩 저장
     */
    @Override
    @Transactional
    public void registersUserBadWordsChat(UserForbiddenWordsChatCreateRequestDto request) {
        List<UserForbiddenWordsChat> entities = request.getForbiddenWords().stream()
                .map(word -> fwRepository.findIdByWord(word)
                        .orElseThrow(() -> new AdminException(
                                ErrorCode.FORBIDDEN_WORD_NOT_FOUND))
                )
                .map(id -> UserForbiddenWordsChat.builder()
                        .userId(request.getUserId())
                        .chatMessageId(request.getChatMessageId())
                        .forbiddenWord(fwRepository.getReferenceById(id))
                        .build()
                )
                .collect(Collectors.toList());

        try {
            chatRepository.saveAll(entities);
        } catch (Exception ex) {
            log.error("금칙어 채팅 기록 저장 실패", ex);
            throw new AdminException(ErrorCode.USER_FORBIDDEN_WORDS_CHAT_SAVE_FAILED);
        }
    }

    /**
     * 사용자의 금칙어 누적 집계
     */
    @Override
    public long countByUserId(Long userId) {
        try {
            return chatRepository.countByUserId(userId);
        } catch (Exception ex) {
            log.error("금칙어 로그 집계 실패, userId={}", userId, ex);
            throw new AdminException(ErrorCode.USER_FORBIDDEN_WORDS_CHAT_AGGREGATE_FAILED);
        }
    }

    /**
     * 금칙어 로그 삭제 후 언밴이 필요하면 처리
     */
    @Override
    @Transactional
    public void deleteAndProcess(Long chatLogId) {
        // 1) 삭제 대상 조회 -> userId 확보
        UserForbiddenWordsChat record = chatRepository.findById(chatLogId)
                .orElseThrow(() -> new AdminException(ErrorCode.USER_FORBIDDEN_WORDS_CHAT_RETRIEVE_FAILED));
        Long userId = record.getUserId();

        // 2) 로그 삭제
        try {
            chatRepository.deleteById(chatLogId);
        } catch (Exception ex) {
            log.error("금칙어 로그 삭제 실패, id={}", chatLogId, ex);
            throw new AdminException(ErrorCode.USER_FORBIDDEN_WORDS_CHAT_DELETE_FAILED);
        }

        // 3) 삭제 후 남은 위반 횟수 집계
        long afterCount;
        try {
            afterCount = chatRepository.countByUserId(userId);
        } catch (Exception ex) {
            log.error("위반 횟수 집계 실패, userId={}", userId, ex);
            throw new AdminException(ErrorCode.USER_FORBIDDEN_WORDS_CHAT_AGGREGATE_FAILED);
        }
        long beforeCount = afterCount + 1;

        // 4) 언밴 조건: beforeCount가 임계치(30,50,100,300,500,1000) 중 하나였고
        //               afterCount가 그 임계치 미만일 때
        int[] thresholds = {30, 50, 100, 300, 500, 1000};
        boolean needsUnban = false;
        for (int t : thresholds) {
            if (beforeCount == t && afterCount < t) {
                needsUnban = true;
                break;
            }
        }
        if (!needsUnban) {
            return;  // 해제 조건 아니면 종료
        }

        // 5) 언밴 API 호출: ACTIVE, unbanTime=null
        UpdateUserStatusRequestDto req = UpdateUserStatusRequestDto.builder()
                .userId(userId)
                .status(Status.ACTIVE)
                .unbanTime(null)
                .build();

        try {
            userClient.put()
                    .uri("/user/status")
                    .bodyValue(req)
                    .retrieve()
                    .bodyToMono(Void.class)
                    .block();
        } catch (Exception ex) {
            log.error("사용자 해제 API 호출 실패, userId={}", userId, ex);
            throw new AdminException(ErrorCode.USER_STATUS_UPDATE_FAILED);
        }
    }
}
