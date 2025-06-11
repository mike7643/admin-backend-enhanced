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
     * GET /admin/forbidden-words/chats/search?searchWord={nameOrEmail}
     */
    @GetMapping("/search")
    public BaseResponseDto<List<UserForbiddenWordsChatDto>> search(
            @RequestParam("searchWord") String searchWord
    ) {
        List<UserForbiddenWordsChatDto> dtos = service.findByUserSearchWord(searchWord);
        return BaseResponseDto.success(dtos);
    }


    /**
     * POST /admin/forbidden-words/chats
     * 단일 금칙어 로그 저장
     *
     * body:
     * {
     *   "userId": 1,
     *   "chatMessageId": 42,
     *   "forbiddenWordId": 10
     * }
     */
    @PostMapping
    public BaseResponseDto<Void> registersUserBadWordsChat(
            @RequestBody UserForbiddenWordsChatCreateRequestDto request
    ) {
        service.registersUserBadWordsChat(request);
        return BaseResponseDto.voidSuccess();
    }


    /**
     * GET /admin/forbidden-words/chats/count?userId={userId}
     * → 특정 사용자의 누적 금칙어 위반 횟수 조회
     */
    @GetMapping("/count")
    public BaseResponseDto<Long> count(@RequestParam("userId") Long userId) {
        long total = service.countByUserId(userId);
        return BaseResponseDto.success(total);
    }
}