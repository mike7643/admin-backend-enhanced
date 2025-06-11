package com.comprehensive.eureka.admin.service;

import com.comprehensive.eureka.admin.dto.UserForbiddenWordsChatDto;
import com.comprehensive.eureka.admin.dto.request.UserForbiddenWordsChatCreateRequestDto;

import java.util.List;

public interface UserForbiddenWordsChatService {

    /**
     * 이름 또는 이메일(searchWord)로 User 검색 → 사용자 금칙어 채팅 기록 조회
     *
     * @param searchWord 검색 키워드 (이름 또는 이메일)
     * @return 사용자 금칙어 채팅 기록 목록 DTO
     */
    List<UserForbiddenWordsChatDto> findByUserSearchWord(String searchWord);

    /**
     * 단일 금칙어 ID로 한 건씩 저장
     */
    void registersUserBadWordsChat(UserForbiddenWordsChatCreateRequestDto request);
}
