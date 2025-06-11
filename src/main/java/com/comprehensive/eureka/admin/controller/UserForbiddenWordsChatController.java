package com.comprehensive.eureka.admin.controller;


import com.comprehensive.eureka.admin.dto.BaseResponseDto;
import com.comprehensive.eureka.admin.dto.UserForbiddenWordsChatDto;
import com.comprehensive.eureka.admin.service.UserForbiddenWordsChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
}
