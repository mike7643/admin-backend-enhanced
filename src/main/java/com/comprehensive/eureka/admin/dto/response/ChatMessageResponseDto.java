package com.comprehensive.eureka.admin.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor

public class ChatMessageResponseDto {
    private Long id;
    private String message;
    private Long sentAt;
}