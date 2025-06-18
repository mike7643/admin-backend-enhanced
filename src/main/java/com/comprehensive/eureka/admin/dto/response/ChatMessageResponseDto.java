package com.comprehensive.eureka.admin.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChatMessageResponseDto {
    private Long id;
    private String message;
    private LocalDateTime sentAt;
}