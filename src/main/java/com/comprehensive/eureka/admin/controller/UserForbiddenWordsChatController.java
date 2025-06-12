package com.comprehensive.eureka.admin.controller;


import com.comprehensive.eureka.admin.dto.BaseResponseDto;
import com.comprehensive.eureka.admin.dto.UserForbiddenWordsChatDto;
import com.comprehensive.eureka.admin.dto.request.UserForbiddenWordsChatCreateRequestDto;
import com.comprehensive.eureka.admin.service.UserForbiddenWordsChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/forbidden-words/chats")
@RequiredArgsConstructor
public class UserForbiddenWordsChatController {

    private final UserForbiddenWordsChatService service;

    /**
     * 특정 사용자 ID로 금칙어 채팅 기록 조회
     */
    @GetMapping
    public BaseResponseDto<List<UserForbiddenWordsChatDto>> getByUserId(
            @RequestParam("userId") Long userId
    ) {
        List<UserForbiddenWordsChatDto> dtos = service.findByUserId(userId);
        return BaseResponseDto.success(dtos);
    }


    /**
     * 다중 금칙어 로그 저장
     */
    @PostMapping
    public BaseResponseDto<Void> registersUserBadWordsChat(
            @RequestBody UserForbiddenWordsChatCreateRequestDto request
    ) {
        service.registersUserBadWordsChat(request);
        return BaseResponseDto.voidSuccess();
    }


    /**
     * 특정 사용자의 누적 금칙어 위반 횟수 조회
     */
    @GetMapping("/count")
    public BaseResponseDto<Long> count(@RequestParam("userId") Long userId) {
        long total = service.countByUserId(userId);
        return BaseResponseDto.success(total);
    }


    /**
     * 금칙어 로그 삭제 후 언밴 처리
     */
    @DeleteMapping("/{chatLogId}")
    public BaseResponseDto<Void> deleteAndProcess(
            @PathVariable Long chatLogId
    ) {
        service.deleteAndProcess(chatLogId);
        return BaseResponseDto.voidSuccess();
    }
}