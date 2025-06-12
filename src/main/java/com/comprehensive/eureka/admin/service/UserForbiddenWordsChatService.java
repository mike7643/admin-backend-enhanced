package com.comprehensive.eureka.admin.service;

import com.comprehensive.eureka.admin.dto.UserForbiddenWordsChatDto;
import com.comprehensive.eureka.admin.dto.request.UserForbiddenWordsChatCreateRequestDto;

import java.util.List;

public interface UserForbiddenWordsChatService {


    /**
     * 특정 사용자 ID로 금칙어 채팅 기록 조회
     */
    List<UserForbiddenWordsChatDto> findByUserId(Long userId);

    /**
     * 단일 금칙어 ID로 한 건씩 저장
     */
    void registersUserBadWordsChat(UserForbiddenWordsChatCreateRequestDto request);

    /**
     * 특정 사용자의 누적 금칙어 수 조회
     */
    long countByUserId(Long userId);

    /**
     * 단일 금칙어 로그 삭제 후
     * 누적 위반 횟수 재계산 -> 정책에 따라 사용자 상태 갱신
     */
    void deleteAndProcess(Long chatLogId);
}
