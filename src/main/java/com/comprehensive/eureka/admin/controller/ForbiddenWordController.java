package com.comprehensive.eureka.admin.controller;

import com.comprehensive.eureka.admin.dto.BaseResponseDto;
import com.comprehensive.eureka.admin.dto.request.ForbiddenWordRequestDto;
import com.comprehensive.eureka.admin.dto.response.ForbiddenWordResponseDto;
import com.comprehensive.eureka.admin.service.ForbiddenWordService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/forbidden-words")
@RequiredArgsConstructor
public class ForbiddenWordController {

    private final ForbiddenWordService forbiddenWordService;

    /**
     * 금칙어 등록
     */
    @PostMapping
    public ResponseEntity<BaseResponseDto> addForbiddenWord(
            @RequestBody ForbiddenWordRequestDto requestDto
    ) {
        ForbiddenWordResponseDto saved = forbiddenWordService.addForbiddenWord(requestDto);
        return ResponseEntity.ok(BaseResponseDto.success(saved));
    }

    /**
     * 금칙어 조회
     * @param used  사용 여부 필터
     * @param value 단어로 필터
     */
    @GetMapping
    public ResponseEntity<BaseResponseDto> getForbiddenWords(
            @RequestParam(value = "used", required = false) Boolean used,
            @RequestParam(value = "value", required = false) String value
    ) {
        List<ForbiddenWordResponseDto> list = forbiddenWordService.getForbiddenWords(used, value);
        return ResponseEntity.ok(BaseResponseDto.success(list));
    }

    /**
     * 금칙어 사용여부 토글
     */
    @PatchMapping("/{wordId}/status-change")
    public ResponseEntity<BaseResponseDto<ForbiddenWordResponseDto>> toggleStatus(
            @PathVariable("wordId") Long id
    ) {
        ForbiddenWordResponseDto dto = forbiddenWordService.toggleForbiddenWordStatus(id);
        return ResponseEntity.ok(BaseResponseDto.success(dto));
    }
}