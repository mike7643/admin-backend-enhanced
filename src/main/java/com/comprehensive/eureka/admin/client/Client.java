package com.comprehensive.eureka.admin.client;


import com.comprehensive.eureka.admin.dto.BaseResponseDto;
import com.comprehensive.eureka.admin.dto.request.ChatMessageRequestDto;
import com.comprehensive.eureka.admin.dto.response.ChatMessageResponseDto;
import com.comprehensive.eureka.admin.util.WebClientUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class Client {
    private final WebClientUtil webClientUtil;

    public BaseResponseDto<ChatMessageResponseDto> registerForbiddenChatLog(ChatMessageRequestDto dto) {
        return webClientUtil.post(
                "http://localhost:8082/chatbot/api/chat/message",
                dto,
                new ParameterizedTypeReference<>() {
                }
        );
    }
}