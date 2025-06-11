package com.comprehensive.eureka.admin.service;

import com.comprehensive.eureka.admin.dto.UserForbiddenWordsChatDto;
import com.comprehensive.eureka.admin.dto.request.UserForbiddenWordsChatCreateRequestDto;
import com.comprehensive.eureka.admin.dto.response.UserInfoResponseDto;
import com.comprehensive.eureka.admin.entity.ForbiddenWord;
import com.comprehensive.eureka.admin.entity.UserForbiddenWordsChat;
import com.comprehensive.eureka.admin.exception.AdminException;
import com.comprehensive.eureka.admin.exception.ErrorCode;
import com.comprehensive.eureka.admin.repository.UserForbiddenWordsChatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
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

    @Qualifier("userClient")
    private final WebClient userClient;


    /**
     * 이름 또는 이메일(searchWord)로 사용자 조회 → 해당 사용자들의 금칙어 채팅 기록 반환
     */
    @Override
    public List<UserForbiddenWordsChatDto> findByUserSearchWord(String searchWord) {
        List<UserInfoResponseDto> users;
        try {
            users = userClient.get()
                    .uri(uri -> uri.path("/user/search")
                            .queryParam("searchWord", searchWord)
                            .build())
                    .retrieve()
                    .bodyToFlux(UserInfoResponseDto.class)
                    .collectList()
                    .block();
        } catch (Exception ex) {
            log.error("사용자 조회 실패", ex);
            throw new AdminException(ErrorCode.USER_FORBIDDEN_WORDS_CHAT_RETRIEVE_FAILED);
        }

        if (users == null || users.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> userIds = users.stream()
                .map(UserInfoResponseDto::getId)
                .distinct()
                .collect(Collectors.toList());

        List<UserForbiddenWordsChat> logs;
        try {
            logs = chatRepository.findByUserIdIn(userIds);
        } catch (Exception ex) {
            log.error("금칙어 채팅 기록 조회 실패", ex);
            throw new AdminException(ErrorCode.USER_FORBIDDEN_WORDS_CHAT_RETRIEVE_FAILED);
        }

        if (logs.isEmpty()) {
            return Collections.emptyList();
        }

        return logs.stream()
                .map(UserForbiddenWordsChatDto::from)
                .collect(Collectors.toList());
    }

    /**
     * 단일 금칙어 ID로 한 건씩 저장
     */
    @Override
    @Transactional
    public void registersUserBadWordsChat(UserForbiddenWordsChatCreateRequestDto request) {
        UserForbiddenWordsChat entity = UserForbiddenWordsChat.builder()
                .userId(request.getUserId())
                .chatMessageId(request.getChatMessageId())
                .forbiddenWord(
                        ForbiddenWord.builder()
                                .id(request.getForbiddenWordId())
                                .build()
                )
                .build();
        try {
            chatRepository.save(entity);
        } catch (Exception ex) {
            log.error("금칙어 채팅 기록 단건 저장 실패", ex);
            throw new AdminException(ErrorCode.USER_FORBIDDEN_WORDS_CHAT_SAVE_FAILED);
        }
    }
}
