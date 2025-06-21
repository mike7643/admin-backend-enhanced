package com.comprehensive.eureka.admin.service;

import com.comprehensive.eureka.admin.dto.AllowWordRequestDto;
import com.comprehensive.eureka.admin.dto.response.AllowWordResponseDto;

public interface AllowWordService {
    AllowWordResponseDto addAllowWord(AllowWordRequestDto dto);
}
