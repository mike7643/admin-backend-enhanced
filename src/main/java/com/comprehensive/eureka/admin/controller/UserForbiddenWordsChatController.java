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
     * 다중 금칙어 로그 저장
     *
     * body:
     * {
     *   "userId": 1,
     *   "chatMessageId": 42,
     *   "forbiddenWords": ["욕1", "욕2", "욕3"]
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


    /**
     * DELETE /admin/forbidden-words/chats/{chatLogId}
     * - 단일 금칙어 로그 삭제
     * - 삭제 후 남은 위반 횟수 변화에 따라 차단 해제 API 호출
     */
    @DeleteMapping("/{chatLogId}")
    public BaseResponseDto<Void> deleteAndProcess(
            @PathVariable Long chatLogId
    ) {
        service.deleteAndProcess(chatLogId);
        return BaseResponseDto.voidSuccess();
    }
}