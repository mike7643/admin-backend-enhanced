package com.comprehensive.eureka.admin.dto.request;

import lombok.*;

/**
 * 단일 금칙어 채팅 기록 저장 요청 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserForbiddenWordsChatCreateRequestDto {
    private Long userId;
    private Long chatMessageId;
    private Long forbiddenWordId;
}
