package com.comprehensive.eureka.admin.service;

import com.comprehensive.eureka.admin.dto.request.ForbiddenWordRequestDto;
import com.comprehensive.eureka.admin.dto.response.ForbiddenWordResponseDto;
import com.comprehensive.eureka.admin.entity.ForbiddenWord;
import com.comprehensive.eureka.admin.exception.AdminException;
import com.comprehensive.eureka.admin.exception.ErrorCode;
import com.comprehensive.eureka.admin.repository.ForbiddenWordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class ForbiddenWordServiceImplTest {

    @Mock
    private ForbiddenWordRepository forbiddenWordRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ForbiddenWordServiceImpl service;

    private final Long ID = 1L;
    private final String WORD = "badword";

    @BeforeEach
    void setup() {
        Mockito.reset(forbiddenWordRepository, eventPublisher);
    }

    @Test
    @DisplayName("getForbiddenWords: 상태+단어 필터 적용")
    void getForbiddenWords_WithStatusAndWord() {
        // given
        ForbiddenWord fw = new ForbiddenWord(ID, WORD, true);
        given(forbiddenWordRepository.findByStatusAndWord(true, WORD))
                .willReturn(List.of(fw));

        // when
        List<ForbiddenWordResponseDto> result = service.getForbiddenWords(true, WORD);

        // then
        assertThat(result)
                .hasSize(1)
                .first()
                .satisfies(dto -> {
                    assertThat(dto.getId()).isEqualTo(ID);
                    assertThat(dto.getWord()).isEqualTo(WORD);
                    assertThat(dto.isStatus()).isTrue();
                });
    }

    @Test
    @DisplayName("getForbiddenWords: 상태만 필터 적용")
    void getForbiddenWords_WithStatusOnly() {
        // given
        ForbiddenWord fw = new ForbiddenWord(ID, WORD, false);
        given(forbiddenWordRepository.findByStatus(false))
                .willReturn(List.of(fw));

        // when
        List<ForbiddenWordResponseDto> result = service.getForbiddenWords(false, null);

        // then
        assertThat(result)
                .hasSize(1)
                .allSatisfy(dto -> {
                    assertThat(dto.getWord()).isEqualTo(WORD);
                    assertThat(dto.isStatus()).isFalse();
                });
    }

    @Test
    @DisplayName("getForbiddenWords: 단어만 필터 적용")
    void getForbiddenWords_WithWordOnly() {
        // given
        ForbiddenWord fw = new ForbiddenWord(ID, WORD, true);
        given(forbiddenWordRepository.findByWord(WORD))
                .willReturn(Optional.of(fw));

        // when
        List<ForbiddenWordResponseDto> result = service.getForbiddenWords(null, WORD);

        // then
        assertThat(result)
                .hasSize(1)
                .extracting(ForbiddenWordResponseDto::getWord)
                .containsExactly(WORD);
    }

    @Test
    @DisplayName("getForbiddenWords: 필터 없음")
    void getForbiddenWords_NoFilter() {
        // given
        ForbiddenWord fw1 = new ForbiddenWord(1L, "foo", true);
        ForbiddenWord fw2 = new ForbiddenWord(2L, "bar", false);
        given(forbiddenWordRepository.findAll())
                .willReturn(List.of(fw1, fw2));

        // when
        List<ForbiddenWordResponseDto> result = service.getForbiddenWords(null, null);

        // then
        assertThat(result)
                .hasSize(2)
                .extracting(ForbiddenWordResponseDto::getWord)
                .containsExactlyInAnyOrder("foo", "bar");
    }

    @Test
    @DisplayName("addForbiddenWord: 이미 존재하면 예외")
    void addForbiddenWord_AlreadyExists() {
        // given
        given(forbiddenWordRepository.existsByWord(WORD)).willReturn(true);
        var req = new ForbiddenWordRequestDto(WORD, true);

        // when / then
        assertThatThrownBy(() -> service.addForbiddenWord(req))
                .isInstanceOf(AdminException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FORBIDDEN_WORD_ALREADY_EXISTS);
    }

    @Test
    @DisplayName("addForbiddenWord: 정상 저장 후 Redis에 등록")
    void addForbiddenWord_Success() {
        // given
        given(forbiddenWordRepository.existsByWord(WORD)).willReturn(false);
        var saved = new ForbiddenWord(ID, WORD, true);
        given(forbiddenWordRepository.save(any(ForbiddenWord.class))).willReturn(saved);
        var req = new ForbiddenWordRequestDto(WORD, true);

        // when
        ForbiddenWordResponseDto dto = service.addForbiddenWord(req);

        // then
        assertThat(dto)
                .extracting(ForbiddenWordResponseDto::getId,
                        ForbiddenWordResponseDto::getWord,
                        ForbiddenWordResponseDto::isStatus)
                .containsExactly(ID, WORD, true);
        then(eventPublisher).should().publishEvent(any(Object.class));
    }

    @Test
    @DisplayName("addForbiddenWord: 저장 중 예외 발생 시 AdminException")
    void addForbiddenWord_SaveThrows() {
        // given
        given(forbiddenWordRepository.existsByWord(WORD)).willReturn(false);
        given(forbiddenWordRepository.save(any()))
                .willThrow(new RuntimeException("DB error"));
        var req = new ForbiddenWordRequestDto(WORD, false);

        // when / then
        assertThatThrownBy(() -> service.addForbiddenWord(req))
                .isInstanceOf(AdminException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FORBIDDEN_WORD_CREATE_FAILED);
    }

    @Test
    @DisplayName("deleteForbiddenWord: ID 없으면 예외")
    void deleteForbiddenWord_NotFound() {
        // given
        given(forbiddenWordRepository.findById(ID)).willReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> service.deleteForbiddenWord(ID))
                .isInstanceOf(AdminException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FORBIDDEN_WORD_NOT_FOUND);
    }

    @Test
    @DisplayName("deleteForbiddenWord: 정상 삭제 시 Redis 동작 검증")
    void deleteForbiddenWord_Success() {
        // given: 상태 true
        ForbiddenWord fwTrue = new ForbiddenWord(ID, WORD, true);
        given(forbiddenWordRepository.findById(ID)).willReturn(Optional.of(fwTrue));

        // when / then
        assertThatCode(() -> service.deleteForbiddenWord(ID))
                .doesNotThrowAnyException();
        then(forbiddenWordRepository).should().delete(fwTrue);
        then(eventPublisher).should().publishEvent(any(Object.class));

        // given: 상태 false
        reset(forbiddenWordRepository, eventPublisher);
        ForbiddenWord fwFalse = new ForbiddenWord(ID, WORD, false);
        given(forbiddenWordRepository.findById(ID)).willReturn(Optional.of(fwFalse));

        // when / then
        assertThatCode(() -> service.deleteForbiddenWord(ID))
                .doesNotThrowAnyException();
        then(forbiddenWordRepository).should().delete(fwFalse);
        then(eventPublisher).should(never()).publishEvent(any(Object.class));
    }

    @Test
    @DisplayName("toggleForbiddenWordStatus: ID 없으면 예외")
    void toggleForbiddenWordStatus_NotFound() {
        // given
        given(forbiddenWordRepository.findById(ID)).willReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> service.toggleForbiddenWordStatus(ID))
                .isInstanceOf(AdminException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FORBIDDEN_WORD_NOT_FOUND);
    }

    @Test
    @DisplayName("toggleForbiddenWordStatus: false -> true 전환")
    void toggleForbiddenWordStatus_FalseToTrue() {
        // given
        ForbiddenWord fw = new ForbiddenWord(ID, WORD, false);
        given(forbiddenWordRepository.findById(ID)).willReturn(Optional.of(fw));
        given(forbiddenWordRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        // when
        ForbiddenWordResponseDto dto = service.toggleForbiddenWordStatus(ID);

        // then
        assertThat(dto.isStatus()).isTrue();
        then(eventPublisher).should().publishEvent(any(Object.class));
    }

    @Test
    @DisplayName("toggleForbiddenWordStatus: true -> false 전환")
    void toggleForbiddenWordStatus_TrueToFalse() {
        // given
        ForbiddenWord fw = new ForbiddenWord(ID, WORD, true);
        given(forbiddenWordRepository.findById(ID)).willReturn(Optional.of(fw));
        given(forbiddenWordRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        // when
        ForbiddenWordResponseDto dto = service.toggleForbiddenWordStatus(ID);

        // then
        assertThat(dto.isStatus()).isFalse();
        then(eventPublisher).should().publishEvent(any(Object.class));
    }

    @Test
    @DisplayName("toggleForbiddenWordStatus: Redis 추가 중 예외 발생")
    void toggleForbiddenWordStatus_RedisAddThrows() {
        // given
        ForbiddenWord fw = new ForbiddenWord(ID, WORD, false);
        given(forbiddenWordRepository.findById(ID)).willReturn(Optional.of(fw));
        given(forbiddenWordRepository.save(any())).willAnswer(inv -> {
            ForbiddenWord u = inv.getArgument(0);
            u.setStatus(true);
            return u;
        });
        willThrow(new RuntimeException("redis error"))
                .given(eventPublisher).publishEvent(any(Object.class));

        // when / then
        assertThatThrownBy(() -> service.toggleForbiddenWordStatus(ID))
                .isInstanceOf(AdminException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FORBIDDEN_WORD_CHATBOT_ADD_FAILED);
    }

    @Test
    @DisplayName("toggleForbiddenWordStatus: Redis 제거 중 예외 발생")
    void toggleForbiddenWordStatus_RedisRemoveThrows() {
        // given
        ForbiddenWord fw = new ForbiddenWord(ID, WORD, true);
        given(forbiddenWordRepository.findById(ID)).willReturn(Optional.of(fw));
        given(forbiddenWordRepository.save(any())).willAnswer(inv -> {
            ForbiddenWord u = inv.getArgument(0);
            u.setStatus(false);
            return u;
        });
        willThrow(new RuntimeException("redis error"))
                .given(eventPublisher).publishEvent(any(Object.class));

        // when / then
        assertThatThrownBy(() -> service.toggleForbiddenWordStatus(ID))
                .isInstanceOf(AdminException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FORBIDDEN_WORD_CHATBOT_DELETE_FAILED);
    }
}
