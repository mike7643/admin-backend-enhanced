package com.comprehensive.eureka.admin.service;

import com.comprehensive.eureka.admin.client.Client;
import com.comprehensive.eureka.admin.dto.BaseResponseDto;
import com.comprehensive.eureka.admin.dto.UserForbiddenWordsChatDetailDto;
import com.comprehensive.eureka.admin.dto.request.ChatMessageRequestDto;
import com.comprehensive.eureka.admin.dto.request.UpdateUserStatusRequestDto;
import com.comprehensive.eureka.admin.dto.request.UserForbiddenWordsChatCreateRequestDto;
import com.comprehensive.eureka.admin.dto.request.UserForbiddenWordsRequestDto;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserForbiddenWordsChatServiceImpl implements UserForbiddenWordsChatService {

    private final UserForbiddenWordsChatRepository chatRepository;
    private final ForbiddenWordRepository fwRepository;

    private final WebClient originClient;


    /**
     * 특정 사용자 ID로 금칙어 채팅 기록 조회
     */
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

    /**
     * 다중 금칙어 ID로 한 건씩 저장
     */
    @Override
    @Transactional
    public void registersUserBadWordsChat(UserForbiddenWordsChatCreateRequestDto request) {
        long beforeCount = chatRepository.countByUserId(request.getUserId());

        Long userId = request.getUserId();
        String messageText = request.getChatMessageText();
        Long sentAt = request.getSentAt();

        List<UserForbiddenWordsChat> entities = new ArrayList<>();

        for (String word : request.getForbiddenWords()) {
            Long fwId = fwRepository.findIdByWord(word)
                    .orElseThrow(() -> new AdminException(ErrorCode.FORBIDDEN_WORD_NOT_FOUND));

            int occurrences = countOccurrences(messageText, word);
            if (occurrences <= 0) continue;

            for (int i = 0; i < occurrences; i++) {
                entities.add(UserForbiddenWordsChat.builder()
                        .userId(userId)
                        .chatMessageText(messageText)
                        .chatSentAt(sentAt)
                        .forbiddenWord(fwRepository.getReferenceById(fwId))
                        .build()
                );
            }
        }

        log.info("userId={}, beforeCount={}, entitiesToSave={}", userId, beforeCount, entities.size());

        try {
            List<UserForbiddenWordsChat> saved = chatRepository.saveAll(entities);
            log.info("savedCount={}", saved.size());
        } catch (Exception ex) {
            log.error("금칙어 채팅 기록 저장 실패", ex);
            throw new AdminException(ErrorCode.USER_FORBIDDEN_WORDS_CHAT_SAVE_FAILED);
        }

        long afterCount = beforeCount + entities.size();
        checkApplyBan(userId, beforeCount, afterCount);
    }

    /**
     * 텍스트에서 특정 단어가 몇 번 등장하는지 세야됨
     */
    private int countOccurrences(String text, String word) {
        if (text == null || word == null || word.isEmpty()) return 0;
        int count = 0;
        int idx = 0;
        while ((idx = text.indexOf(word, idx)) != -1) {
            count++;
            idx += word.length();
        }
        return count;
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
            originClient.put()
                    .uri("http://localhost:8085/user/status")
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
            originClient.put()
                    .uri("http://localhost:8085/user/status")
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
