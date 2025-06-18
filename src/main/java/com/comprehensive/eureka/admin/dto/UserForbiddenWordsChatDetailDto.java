package com.comprehensive.eureka.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserForbiddenWordsChatDetailDto {
    private Long userId;

    private String forbiddenWord;

    private String chatMessage;

    private Long chatSentAt;
}