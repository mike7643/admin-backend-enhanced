package com.comprehensive.eureka.admin.service;

import com.comprehensive.eureka.admin.dto.UserForbiddenWordsChatDto;
import com.comprehensive.eureka.admin.dto.response.UserInfoResponseDto;
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

/**
 * 사용자 금칙어 채팅 기록 조회 서비스 구현체
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserForbiddenWordsChatServiceImpl implements UserForbiddenWordsChatService {

    private final UserForbiddenWordsChatRepository repository;

    @Qualifier("userClient")
    private final WebClient userClient;

    @Value("${services.base-url}")
    private String baseUrl;

    /**
     * 이름 또는 이메일(searchWord)로 User 검색 → 사용자 ID 목록으로 로그 조회
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
            logs = repository.findByUserIdIn(userIds);
        } catch (Exception ex) {
            throw new AdminException(ErrorCode.USER_FORBIDDEN_WORDS_CHAT_RETRIEVE_FAILED);
        }

        if (logs.isEmpty()) {
            return Collections.emptyList();
        }

        return logs.stream()
                .map(UserForbiddenWordsChatDto::from)
                .collect(Collectors.toList());
    }
}
