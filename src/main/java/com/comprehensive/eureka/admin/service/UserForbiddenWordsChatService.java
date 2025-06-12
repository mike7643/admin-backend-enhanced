package com.comprehensive.eureka.admin.service;

import com.comprehensive.eureka.admin.dto.UserForbiddenWordsChatDto;
import com.comprehensive.eureka.admin.dto.request.UserForbiddenWordsChatCreateRequestDto;

import java.util.List;

public interface UserForbiddenWordsChatService {

    List<UserForbiddenWordsChatDto> findByUserId(Long userId);

    void registersUserBadWordsChat(UserForbiddenWordsChatCreateRequestDto request);

    long countByUserId(Long userId);

    void deleteAndProcess(Long chatLogId);
}
