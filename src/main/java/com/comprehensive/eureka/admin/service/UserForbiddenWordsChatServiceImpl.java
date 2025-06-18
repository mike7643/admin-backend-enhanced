package com.comprehensive.eureka.admin.service;

import com.comprehensive.eureka.admin.dto.UserForbiddenWordsChatDetailDto;
import com.comprehensive.eureka.admin.dto.request.ChatMessageRequestDto;
import com.comprehensive.eureka.admin.dto.request.UpdateUserStatusRequestDto;
import com.comprehensive.eureka.admin.dto.request.UserForbiddenWordsChatCreateRequestDto;
import com.comprehensive.eureka.admin.dto.response.ChatMessageResponseDto;
import com.comprehensive.eureka.admin.entity.UserForbiddenWordsChat;
import com.comprehensive.eureka.admin.enums.Status;
import com.comprehensive.eureka.admin.exception.AdminException;
import com.comprehensive.eureka.admin.exception.ErrorCode;
import com.comprehensive.eureka.admin.repository.ForbiddenWordRepository;
import com.comprehensive.eureka.admin.repository.UserForbiddenWordsChatRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;

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

    @Qualifier("chatbotClient")
    private final WebClient chatClient;
    /**
     * 특정 사용자 ID로 금칙어 채팅 기록 조회
     */
    @Override
    public List<UserForbiddenWordsChatDetailDto> findDetailByUserId(Long userId) {
        List<UserForbiddenWordsChat> logs;
        try {
            logs = chatRepository.findByUserId(userId);
        } catch (Exception ex) {
            log.error("금칙어 채팅 기록 조회 실패 (userId={})", userId, ex);
            throw new AdminException(ErrorCode.USER_FORBIDDEN_WORDS_CHAT_RETRIEVE_FAILED);
        }

        if (logs.isEmpty()) {
            return Collections.emptyList();
        }
        return logs.stream()
                .map(log -> new UserForbiddenWordsChatDetailDto(
                        log.getUserId(),
                        log.getForbiddenWord().getWord(),
                        log.getChatMessageText(),
                        log.getChatSentAt()
                ))
                .collect(Collectors.toList());
    }

    /**
     * 다중 금칙어 ID로 한 건씩 저장
     */
    @Override
    @Transactional
    public void registersUserBadWordsChat(UserForbiddenWordsChatCreateRequestDto request) {
        long beforeCount = chatRepository.countByUserId(request.getUserId());

        ChatMessageResponseDto chatDto = chatClient.post()
                .uri("/api/chat/messages/detail")
                .bodyValue(new ChatMessageRequestDto(request.getChatMessageId()))
                .retrieve()
                .bodyToMono(ChatMessageResponseDto.class)
                .block();

        if (chatDto == null) {
            log.error("채팅 메시지 조회 실패, id={}", request.getChatMessageId());
            throw new AdminException(ErrorCode.CHAT_MESSAGE_RETRIEVE_FAILED);
        }

        String messageText = chatDto.getMessage();
        LocalDateTime sentAt = chatDto.getSentAt();

        List<UserForbiddenWordsChat> entities = request.getForbiddenWords().stream()
                .map(word -> fwRepository.findIdByWord(word)
                        .orElseThrow(() -> new AdminException(ErrorCode.FORBIDDEN_WORD_NOT_FOUND)))
                .map(fwId -> UserForbiddenWordsChat.builder()
                        .userId(request.getUserId())
                        .chatMessageId(request.getChatMessageId())
                        .forbiddenWord(fwRepository.getReferenceById(fwId))
                        .chatMessageText(messageText)
                        .chatSentAt(sentAt)
                        .build()
                )
                .collect(Collectors.toList());
        try {
            chatRepository.saveAll(entities);
        } catch (Exception ex) {
            log.error("금칙어 채팅 기록 저장 실패", ex);
            throw new AdminException(ErrorCode.USER_FORBIDDEN_WORDS_CHAT_SAVE_FAILED);
        }

        long afterCount = beforeCount + entities.size();
        log.info("userId {} 의 금칙어 위반 횟수 = {}", request.getUserId(), afterCount);

        checkApplyBan(request.getUserId(), beforeCount, afterCount);
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

    private void checkApplyBan(Long userId, long beforeCount, long afterCount) {
        int[] thresholds = {1000, 500, 300, 100, 50, 30};
        int targetThreshold = 0;

        for (int t : thresholds) {
            if (beforeCount < t && afterCount >= t) {
                targetThreshold = t;
                break;
            }
        }

        if (targetThreshold == 0) return;

        LocalDateTime unbanTime = null;
        LocalDate banEndDate = null;

        switch (targetThreshold) {
            case 30: // 당일 자정
                banEndDate = LocalDate.now();
                break;
            case 50: // 다음날 자정
                banEndDate = LocalDate.now().plusDays(1);
                break;
            case 100: // 일주일 후 자정
                banEndDate = LocalDate.now().plusWeeks(1);
                break;
            case 300: // 2주 후 자정
                banEndDate = LocalDate.now().plusWeeks(2);
                break;
            case 500: // 한달 후 자정
                banEndDate = LocalDate.now().plusMonths(1);
                break;
            default: // 영구 정지
                unbanTime = LocalDateTime.of(9999, 12, 31, 23, 59, 59);
                break;
        }

        if (banEndDate != null) unbanTime = banEndDate.plusDays(1).atStartOfDay();

        UpdateUserStatusRequestDto req = UpdateUserStatusRequestDto.builder()
                .userId(userId)
                .status(Status.INACTIVE)
                .unbanTime(unbanTime)
                .build();

        try {
            userClient.put()
                    .uri("/user/status")
                    .bodyValue(req)
                    .retrieve()
                    .bodyToMono(Void.class)
                    .block();

        } catch (Exception ex) {
            log.error("사용자 차단 API 호출 실패, userId={}", userId, ex);
            throw new AdminException(ErrorCode.USER_STATUS_UPDATE_FAILED);
        }
    }
}
