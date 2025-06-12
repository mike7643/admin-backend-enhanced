package com.comprehensive.eureka.admin.dto;

import com.comprehensive.eureka.admin.entity.UserForbiddenWordsChat;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserForbiddenWordsChatDto {
    private Long id;
    private Long userId;
    private Long forbiddenWordId;
    private Long chatMessageId;

    public static UserForbiddenWordsChatDto from(UserForbiddenWordsChat e) {
        return UserForbiddenWordsChatDto.builder()
            .id(e.getId())
            .userId(e.getUserId())
            .forbiddenWordId(e.getForbiddenWord().getId())
            .chatMessageId(e.getChatMessageId())
            .build();
    }
}
