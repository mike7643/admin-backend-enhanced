package com.comprehensive.eureka.admin.service;

import com.comprehensive.eureka.admin.dto.AllowWordRequestDto;
import com.comprehensive.eureka.admin.dto.response.AllowWordResponseDto;
import com.comprehensive.eureka.admin.dto.response.ForbiddenWordResponseDto;
import java.util.List;

public interface AllowWordService {
    AllowWordResponseDto addAllowWord(AllowWordRequestDto dto);

    List<AllowWordResponseDto> getAllowWords(Boolean used, String value);

    void deleteAllowWord(Long id);

    AllowWordResponseDto toggleAllowWordStatus(Long id);
}
