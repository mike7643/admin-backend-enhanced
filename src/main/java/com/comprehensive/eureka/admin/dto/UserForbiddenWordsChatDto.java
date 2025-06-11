package com.comprehensive.eureka.admin.dto;

import com.comprehensive.eureka.admin.entity.UserForbiddenWordsChat;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 사용자_금칙어_채팅 기록 조회 결과용 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserForbiddenWordsChatDto {
    private Long id;
    private Long userId;
    private Long forbiddenWordId;
    private Long chatMessageId;

    /**
     * Entity → DTO 변환
     */
    public static UserForbiddenWordsChatDto from(UserForbiddenWordsChat e) {
        return UserForbiddenWordsChatDto.builder()
            .id(e.getId())
            .userId(e.getUserId())
            .forbiddenWordId(e.getForbiddenWord().getId())
            .chatMessageId(e.getChatMessageId())
            .build();
    }
}
