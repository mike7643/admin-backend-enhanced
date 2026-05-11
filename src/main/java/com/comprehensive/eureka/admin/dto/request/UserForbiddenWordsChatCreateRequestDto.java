package com.comprehensive.eureka.admin.dto.request;

import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserForbiddenWordsChatCreateRequestDto {
    private Long userId;
    private String chatMessageText;
    private Long sentAt;
    private List<String> forbiddenWords;
    private String eventId;
}