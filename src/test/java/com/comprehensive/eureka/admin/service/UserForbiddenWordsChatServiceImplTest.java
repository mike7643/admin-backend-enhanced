package com.comprehensive.eureka.admin.service;

import com.comprehensive.eureka.admin.dto.UserForbiddenWordsChatDetailDto;
import com.comprehensive.eureka.admin.dto.request.UserForbiddenWordsChatCreateRequestDto;
import com.comprehensive.eureka.admin.dto.request.UserForbiddenWordsRequestDto;
import com.comprehensive.eureka.admin.entity.ForbiddenWord;
import com.comprehensive.eureka.admin.entity.UserForbiddenWordsChat;
import com.comprehensive.eureka.admin.exception.AdminException;
import com.comprehensive.eureka.admin.exception.ErrorCode;
import com.comprehensive.eureka.admin.repository.UserForbiddenWordsChatRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class UserForbiddenWordsChatServiceImplTest {

    @Mock
    private UserForbiddenWordsChatRepository chatRepository;

    @Mock
    private RegisterForbiddenWordsChatUseCase registerUseCase;

    @Mock
    private DeleteForbiddenWordsChatUseCase deleteUseCase;

    @InjectMocks
    private UserForbiddenWordsChatServiceImpl service;

    @Test
    @DisplayName("findDetailByUserId: 정상 조회")
    void findDetailByUserId_Success() {
        UserForbiddenWordsRequestDto req = new UserForbiddenWordsRequestDto(42L);
        UserForbiddenWordsChat chat = UserForbiddenWordsChat.builder()
                .userId(42L)
                .forbiddenWord(new ForbiddenWord(1L, "bad", true))
                .chatMessageText("msg")
                .chatSentAt(10L)
                .build();
        given(chatRepository.findByUserIdOrderByChatSentAtDesc(42L)).willReturn(List.of(chat));

        List<UserForbiddenWordsChatDetailDto> result = service.findDetailByUserId(req);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getForbiddenWord()).isEqualTo("bad");
    }

    @Test
    @DisplayName("findDetailByUserId: 빈 기록이면 빈 리스트 반환")
    void findDetailByUserId_Empty() {
        given(chatRepository.findByUserIdOrderByChatSentAtDesc(42L)).willReturn(Collections.emptyList());
        List<UserForbiddenWordsChatDetailDto> result = service.findDetailByUserId(new UserForbiddenWordsRequestDto(42L));
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findDetailByUserId: 조회 중 예외 발생")
    void findDetailByUserId_Throws() {
        given(chatRepository.findByUserIdOrderByChatSentAtDesc(42L)).willThrow(new RuntimeException("DB error"));
        assertThatThrownBy(() -> service.findDetailByUserId(new UserForbiddenWordsRequestDto(42L)))
                .isInstanceOf(AdminException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_FORBIDDEN_WORDS_CHAT_RETRIEVE_FAILED);
    }

    @Test
    @DisplayName("registersUserBadWordsChat: 등록 UseCase에 위임")
    void registersUserBadWordsChat_Delegates() {
        UserForbiddenWordsChatCreateRequestDto req = UserForbiddenWordsChatCreateRequestDto.builder()
                .userId(42L)
                .build();
        assertThatCode(() -> service.registersUserBadWordsChat(req)).doesNotThrowAnyException();
        then(registerUseCase).should().execute(req);
    }

    @Test
    @DisplayName("countByUserId: 정상 집계")
    void countByUserId_Success() {
        given(chatRepository.countByUserId(42L)).willReturn(5L);
        assertThat(service.countByUserId(42L)).isEqualTo(5L);
    }

    @Test
    @DisplayName("deleteAndProcess: 삭제 UseCase에 위임")
    void deleteAndProcess_Delegates() {
        assertThatCode(() -> service.deleteAndProcess(100L)).doesNotThrowAnyException();
        then(deleteUseCase).should().execute(100L);
    }
}
