package com.comprehensive.eureka.admin.service;

import com.comprehensive.eureka.admin.dto.UserForbiddenWordsChatDetailDto;
import com.comprehensive.eureka.admin.dto.request.UserForbiddenWordsChatCreateRequestDto;

import java.util.List;

public interface UserForbiddenWordsChatService {

    List<UserForbiddenWordsChatDetailDto> findDetailByUserId(Long userId);

    void registersUserBadWordsChat(UserForbiddenWordsChatCreateRequestDto request);

    long countByUserId(Long userId);

    void deleteAndProcess(Long chatLogId);
}
