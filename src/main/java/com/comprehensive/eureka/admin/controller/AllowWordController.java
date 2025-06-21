package com.comprehensive.eureka.admin.controller;

import com.comprehensive.eureka.admin.dto.AllowWordRequestDto;
import com.comprehensive.eureka.admin.dto.BaseResponseDto;
import com.comprehensive.eureka.admin.dto.request.ForbiddenWordRequestDto;
import com.comprehensive.eureka.admin.dto.response.AllowWordResponseDto;
import com.comprehensive.eureka.admin.dto.response.ForbiddenWordResponseDto;
import com.comprehensive.eureka.admin.service.AllowWordService;
import com.comprehensive.eureka.admin.service.ForbiddenWordService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/allow-words")
@RequiredArgsConstructor
public class AllowWordController {

    private final AllowWordService allowWordService;

    /**
     * 금칙어 등록
     */
    @PostMapping
    public ResponseEntity<BaseResponseDto> addForbiddenWord(
            @RequestBody AllowWordRequestDto requestDto
    ) {
        AllowWordResponseDto saved = allowWordService.addAllowWord(requestDto);
        return ResponseEntity.ok(BaseResponseDto.success(saved));
    }

}