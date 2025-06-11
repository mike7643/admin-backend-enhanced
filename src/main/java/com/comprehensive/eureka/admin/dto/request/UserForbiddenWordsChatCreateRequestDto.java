package com.comprehensive.eureka.admin.dto.request;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserForbiddenWordsChatCreateRequestDto {
    private Long userId;
    private Long chatMessageId;
    private Long forbiddenWordId;
}