package com.comprehensive.eureka.admin.service;

import com.comprehensive.eureka.admin.dto.request.ForbiddenWordRequestDto;
import com.comprehensive.eureka.admin.dto.response.ForbiddenWordResponseDto;

import java.util.List;

public interface ForbiddenWordService {

    ForbiddenWordResponseDto addForbiddenWord(ForbiddenWordRequestDto dto);

    List<ForbiddenWordResponseDto> getForbiddenWords(Boolean used, String value);

    void deleteForbiddenWord(Long id);

    ForbiddenWordResponseDto toggleForbiddenWordStatus(Long id);
}
