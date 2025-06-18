package com.comprehensive.eureka.admin.service;

import com.comprehensive.eureka.admin.dto.UserForbiddenWordsChatDetailDto;
import com.comprehensive.eureka.admin.dto.request.UserForbiddenWordsChatCreateRequestDto;
import com.comprehensive.eureka.admin.dto.request.UserForbiddenWordsRequestDto;

import java.util.List;

public interface UserForbiddenWordsChatService {

    List<UserForbiddenWordsChatDetailDto> findDetailByUserId(UserForbiddenWordsRequestDto request);

    void registersUserBadWordsChat(UserForbiddenWordsChatCreateRequestDto request);

    long countByUserId(Long userId);

    void deleteAndProcess(Long chatLogId);
}
