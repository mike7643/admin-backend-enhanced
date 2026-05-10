package com.comprehensive.eureka.admin.service;

import com.comprehensive.eureka.admin.dto.UserForbiddenWordsChatDetailDto;
import com.comprehensive.eureka.admin.dto.request.UserForbiddenWordsChatCreateRequestDto;
import com.comprehensive.eureka.admin.dto.request.UserForbiddenWordsRequestDto;
import com.comprehensive.eureka.admin.entity.UserForbiddenWordsChat;
import com.comprehensive.eureka.admin.entity.ForbiddenWord;
import com.comprehensive.eureka.admin.exception.AdminException;
import com.comprehensive.eureka.admin.exception.ErrorCode;
import com.comprehensive.eureka.admin.repository.ForbiddenWordRepository;
import com.comprehensive.eureka.admin.repository.UserForbiddenWordsChatRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class UserForbiddenWordsChatServiceImplTest {

    @Mock
    private UserForbiddenWordsChatRepository chatRepository;

    @Mock
    private ForbiddenWordRepository fwRepository;

    @Mock
    private WebClient originClient;

    @InjectMocks
    private UserForbiddenWordsChatServiceImpl service;

    private final Long USER_ID = 42L;
    private final Long CHAT_ID = 100L;
    private final String MESSAGE = "this is a bad word test";
    private final Long SENT_AT = 123456789L;
    private final String WORD = "bad";

    @BeforeEach
    void setUp() {
        Mockito.reset(chatRepository, fwRepository, originClient);
    }

    @Test
    @DisplayName("findDetailByUserId: 정상 조회")
    void findDetailByUserId_Success() {
        // given
        UserForbiddenWordsRequestDto req = new UserForbiddenWordsRequestDto(USER_ID);
        UserForbiddenWordsChat chat = mock(UserForbiddenWordsChat.class);
        ForbiddenWord fw = new ForbiddenWord(1L, WORD, true);
        given(chat.getId()).willReturn(CHAT_ID);
        given(chat.getUserId()).willReturn(USER_ID);
        given(chat.getChatMessageText()).willReturn(MESSAGE);
        given(chat.getChatSentAt()).willReturn(SENT_AT);
        given(chat.getForbiddenWord()).willReturn(fw);
        given(chatRepository.findByUserIdOrderByChatSentAtDesc(USER_ID))
                .willReturn(List.of(chat));

        // when
        List<UserForbiddenWordsChatDetailDto> result = service.findDetailByUserId(req);

        // then
        assertThat(result)
                .hasSize(1)
                .first()
                .satisfies(dto -> {
                    assertThat(dto.getId()).isEqualTo(CHAT_ID);
                    assertThat(dto.getUserId()).isEqualTo(USER_ID);
                    assertThat(dto.getForbiddenWord()).isEqualTo(WORD);
                    assertThat(dto.getChatMessage()).isEqualTo(MESSAGE);
                    assertThat(dto.getChatSentAt()).isEqualTo(SENT_AT);
                });
    }

    @Test
    @DisplayName("findDetailByUserId: 조회 중 예외 발생")
    void findDetailByUserId_RepositoryThrows() {
        // given
        UserForbiddenWordsRequestDto req = new UserForbiddenWordsRequestDto(USER_ID);
        given(chatRepository.findByUserIdOrderByChatSentAtDesc(USER_ID))
                .willThrow(new RuntimeException("DB error"));

        // when / then
        assertThatThrownBy(() -> service.findDetailByUserId(req))
                .isInstanceOf(AdminException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_FORBIDDEN_WORDS_CHAT_RETRIEVE_FAILED);
    }

    @Test
    @DisplayName("findDetailByUserId: 빈 기록이면 빈 리스트 반환")
    void findDetailByUserId_Empty() {
        // given
        UserForbiddenWordsRequestDto req = new UserForbiddenWordsRequestDto(USER_ID);
        given(chatRepository.findByUserIdOrderByChatSentAtDesc(USER_ID))
                .willReturn(Collections.emptyList());

        // when
        List<UserForbiddenWordsChatDetailDto> result = service.findDetailByUserId(req);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("registersUserBadWordsChat: 정상 저장 (기준치 미달)")
    void registersUserBadWordsChat_Success() {
        // given
        List<String> words = List.of(WORD, WORD + "2");
        UserForbiddenWordsChatCreateRequestDto req = UserForbiddenWordsChatCreateRequestDto.builder()
                .userId(USER_ID)
                .chatMessageText(MESSAGE)
                .sentAt(SENT_AT)
                .forbiddenWords(words)
                .build();

        given(chatRepository.countByUserId(USER_ID)).willReturn(0L, 2L);
        given(fwRepository.findIdByWord(WORD)).willReturn(Optional.of(1L));
        given(fwRepository.findIdByWord(WORD + "2")).willReturn(Optional.of(2L));
        given(fwRepository.getReferenceById(1L)).willReturn(new ForbiddenWord(1L, WORD, true));
        given(fwRepository.getReferenceById(2L)).willReturn(new ForbiddenWord(2L, WORD + "2", true));
        given(chatRepository.saveAll(anyList()))
                .willReturn(List.of()); // 실제 저장된 개수는 테스트 대상 아님

        // when
        assertThatCode(() -> service.registersUserBadWordsChat(req))
                .doesNotThrowAnyException();

        // then
        then(chatRepository).should(times(2)).countByUserId(USER_ID);
        then(fwRepository).should(times(words.size())).findIdByWord(anyString());
        then(chatRepository).should().saveAll(argThat(iterable -> {
            int count = 0;
            for (UserForbiddenWordsChat ignored : iterable) {
                count++;
            }
            return count == words.size();
        }));        verifyNoInteractions(originClient);
    }

    @Test
    @DisplayName("registersUserBadWordsChat: 요청 내 중복 단어도 그대로 저장")
    void registersUserBadWordsChat_DuplicateWordsAreCounted() {
        // given
        List<String> words = List.of(WORD, WORD, WORD);
        UserForbiddenWordsChatCreateRequestDto req = UserForbiddenWordsChatCreateRequestDto.builder()
                .userId(USER_ID)
                .chatMessageText(MESSAGE)
                .sentAt(SENT_AT)
                .forbiddenWords(words)
                .build();

        given(chatRepository.countByUserId(USER_ID)).willReturn(10L, 13L);
        given(fwRepository.findIdByWord(WORD)).willReturn(Optional.of(1L));
        given(fwRepository.getReferenceById(1L)).willReturn(new ForbiddenWord(1L, WORD, true));
        given(chatRepository.saveAll(anyList())).willReturn(List.of());

        // when
        assertThatCode(() -> service.registersUserBadWordsChat(req)).doesNotThrowAnyException();

        // then
        then(fwRepository).should(times(3)).findIdByWord(WORD);
        then(chatRepository).should().saveAll(argThat(iterable -> {
            int count = 0;
            for (UserForbiddenWordsChat ignored : iterable) {
                count++;
            }
            return count == 3;
        }));
    }

    @Test
    @DisplayName("registersUserBadWordsChat: 금칙어 목록 비어있으면 저장하지 않음")
    void registersUserBadWordsChat_EmptyForbiddenWords() {
        // given
        UserForbiddenWordsChatCreateRequestDto req = UserForbiddenWordsChatCreateRequestDto.builder()
                .userId(USER_ID)
                .chatMessageText(MESSAGE)
                .sentAt(SENT_AT)
                .forbiddenWords(List.of())
                .build();
        given(chatRepository.countByUserId(USER_ID)).willReturn(0L);

        // when
        assertThatCode(() -> service.registersUserBadWordsChat(req)).doesNotThrowAnyException();

        // then
        then(chatRepository).should(times(1)).countByUserId(USER_ID);
        then(chatRepository).should(never()).saveAll(anyList());
        then(fwRepository).shouldHaveNoInteractions();
        verifyNoInteractions(originClient);
    }

    @Test
    @DisplayName("registersUserBadWordsChat: 금칙어 미존재 예외")
    void registersUserBadWordsChat_ForbiddenWordNotFound() {
        // given
        List<String> words = List.of("unknown");
        UserForbiddenWordsChatCreateRequestDto req = UserForbiddenWordsChatCreateRequestDto.builder()
                .userId(USER_ID)
                .chatMessageText(MESSAGE)
                .sentAt(SENT_AT)
                .forbiddenWords(words)
                .build();
        given(chatRepository.countByUserId(USER_ID)).willReturn(0L);
        given(fwRepository.findIdByWord("unknown")).willReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> service.registersUserBadWordsChat(req))
                .isInstanceOf(AdminException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FORBIDDEN_WORD_NOT_FOUND);
        then(chatRepository).should(times(1)).countByUserId(USER_ID);
        then(fwRepository).should().findIdByWord("unknown");
        then(chatRepository).should(never()).saveAll(anyList());
    }

    @Test
    @DisplayName("registersUserBadWordsChat: 저장 실패 예외")
    void registersUserBadWordsChat_SaveAllThrows() {
        // given
        List<String> words = List.of(WORD);
        UserForbiddenWordsChatCreateRequestDto req = UserForbiddenWordsChatCreateRequestDto.builder()
                .userId(USER_ID)
                .chatMessageText(MESSAGE)
                .sentAt(SENT_AT)
                .forbiddenWords(words)
                .build();

        given(chatRepository.countByUserId(USER_ID)).willReturn(0L);
        given(fwRepository.findIdByWord(WORD)).willReturn(Optional.of(1L));
        given(fwRepository.getReferenceById(1L)).willReturn(new ForbiddenWord(1L, WORD, true));
        willThrow(new RuntimeException("DB error"))
                .given(chatRepository).saveAll(anyList());

        // when / then
        assertThatThrownBy(() -> service.registersUserBadWordsChat(req))
                .isInstanceOf(AdminException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_FORBIDDEN_WORDS_CHAT_SAVE_FAILED);
    }

    @Test
    @DisplayName("countByUserId: 정상 집계")
    void countByUserId_Success() {
        // given
        given(chatRepository.countByUserId(USER_ID)).willReturn(5L);

        // when
        long count = service.countByUserId(USER_ID);

        // then
        assertThat(count).isEqualTo(5L);
    }

    @Test
    @DisplayName("countByUserId: 집계 실패 예외")
    void countByUserId_Throws() {
        // given
        given(chatRepository.countByUserId(USER_ID))
                .willThrow(new RuntimeException("DB error"));

        // when / then
        assertThatThrownBy(() -> service.countByUserId(USER_ID))
                .isInstanceOf(AdminException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_FORBIDDEN_WORDS_CHAT_AGGREGATE_FAILED);
    }

    @Test
    @DisplayName("deleteAndProcess: 로그 없음 예외")
    void deleteAndProcess_NotFound() {
        // given
        given(chatRepository.findById(CHAT_ID)).willReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> service.deleteAndProcess(CHAT_ID))
                .isInstanceOf(AdminException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_FORBIDDEN_WORDS_CHAT_RETRIEVE_FAILED);
    }

    @Test
    @DisplayName("deleteAndProcess: 삭제 실패 예외")
    void deleteAndProcess_DeleteThrows() {
        // given
        UserForbiddenWordsChat record = mock(UserForbiddenWordsChat.class);
        given(record.getUserId()).willReturn(USER_ID);
        given(chatRepository.findById(CHAT_ID)).willReturn(Optional.of(record));
        willThrow(new RuntimeException("DB error"))
                .given(chatRepository).deleteById(CHAT_ID);

        // when / then
        assertThatThrownBy(() -> service.deleteAndProcess(CHAT_ID))
                .isInstanceOf(AdminException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_FORBIDDEN_WORDS_CHAT_DELETE_FAILED);
    }

    @Test
    @DisplayName("deleteAndProcess: 집계 실패 예외")
    void deleteAndProcess_AggregateThrows() {
        // given
        UserForbiddenWordsChat record = mock(UserForbiddenWordsChat.class);
        given(record.getUserId()).willReturn(USER_ID);
        given(chatRepository.findById(CHAT_ID)).willReturn(Optional.of(record));
        willDoNothing().given(chatRepository).deleteById(CHAT_ID);
        given(chatRepository.countByUserId(USER_ID))
                .willThrow(new RuntimeException("DB error"));

        // when / then
        assertThatThrownBy(() -> service.deleteAndProcess(CHAT_ID))
                .isInstanceOf(AdminException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_FORBIDDEN_WORDS_CHAT_AGGREGATE_FAILED);
    }

    @Test
    @DisplayName("deleteAndProcess: 언밴 조건 미충족 시 정상 반환")
    void deleteAndProcess_NoUnban() {
        // given
        UserForbiddenWordsChat record = mock(UserForbiddenWordsChat.class);
        given(record.getUserId()).willReturn(USER_ID);
        given(chatRepository.findById(CHAT_ID)).willReturn(Optional.of(record));
        willDoNothing().given(chatRepository).deleteById(CHAT_ID);
        // afterCount +1 != any threshold
        given(chatRepository.countByUserId(USER_ID)).willReturn(5L);

        // when / then
        assertThatCode(() -> service.deleteAndProcess(CHAT_ID))
                .doesNotThrowAnyException();
        verifyNoInteractions(originClient);
    }
}
