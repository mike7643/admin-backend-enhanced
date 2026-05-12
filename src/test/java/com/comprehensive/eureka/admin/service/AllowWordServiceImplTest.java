package com.comprehensive.eureka.admin.service;

import com.comprehensive.eureka.admin.dto.AllowWordRequestDto;
import com.comprehensive.eureka.admin.dto.response.AllowWordResponseDto;
import com.comprehensive.eureka.admin.entity.AllowWord;
import com.comprehensive.eureka.admin.exception.AdminException;
import com.comprehensive.eureka.admin.exception.ErrorCode;
import com.comprehensive.eureka.admin.repository.AllowWordRepository;
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
class AllowWordServiceImplTest {

    @Mock
    private AllowWordRepository allowWordRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private AllowWordServiceImpl service;

    private final Long ID = 1L;
    private final String WORD = "allowword";

    @BeforeEach
    void setup() {
        Mockito.reset(allowWordRepository, eventPublisher);
    }

    @Test
    @DisplayName("addAllowWord: 이미 존재하면 예외")
    void addAllowWord_AlreadyExists() {
        // given
        given(allowWordRepository.existsByWord(WORD)).willReturn(true);
        var req = new AllowWordRequestDto(WORD, true);

        // when / then
        assertThatThrownBy(() -> service.addAllowWord(req))
                .isInstanceOf(AdminException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ALLOW_WORD_ALREADY_EXISTS);
    }

    @Test
    @DisplayName("addAllowWord: 정상 저장 후 Redis에 등록")
    void addAllowWord_Success() {
        // given
        given(allowWordRepository.existsByWord(WORD)).willReturn(false);
        var saved = new AllowWord(ID, WORD, true);
        given(allowWordRepository.save(any(AllowWord.class))).willReturn(saved);
        var req = new AllowWordRequestDto(WORD, true);

        // when
        AllowWordResponseDto dto = service.addAllowWord(req);

        // then
        assertThat(dto)
                .extracting(AllowWordResponseDto::getId,
                        AllowWordResponseDto::getWord,
                        AllowWordResponseDto::isStatus)
                .containsExactly(ID, WORD, true);
        then(eventPublisher).should().publishEvent(any(Object.class));
    }

    @Test
    @DisplayName("addAllowWord: 저장 중 예외 발생 시 AdminException")
    void addAllowWord_SaveThrows() {
        // given
        given(allowWordRepository.existsByWord(WORD)).willReturn(false);
        given(allowWordRepository.save(any()))
                .willThrow(new RuntimeException("DB error"));
        var req = new AllowWordRequestDto(WORD, false);

        // when / then
        assertThatThrownBy(() -> service.addAllowWord(req))
                .isInstanceOf(AdminException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ALLOW_WORD_CREATE_FAILED);
    }

    @Test
    @DisplayName("getAllowWords: 사용 여부+단어 필터 적용")
    void getAllowWords_WithStatusAndWord() {
        // given
        AllowWord aw = new AllowWord(ID, WORD, true);
        given(allowWordRepository.findByStatusAndWord(true, WORD))
                .willReturn(Optional.of(List.of(aw)));

        // when
        List<AllowWordResponseDto> result = service.getAllowWords(true, WORD);

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
    @DisplayName("getAllowWords: 상태만 필터 적용")
    void getAllowWords_WithStatusOnly() {
        // given
        AllowWord aw = new AllowWord(ID, WORD, false);
        given(allowWordRepository.findByStatus(false))
                .willReturn(List.of(aw));

        // when
        List<AllowWordResponseDto> result = service.getAllowWords(false, null);

        // then
        assertThat(result)
                .hasSize(1)
                .allSatisfy(dto -> {
                    assertThat(dto.getWord()).isEqualTo(WORD);
                    assertThat(dto.isStatus()).isFalse();
                });
    }

    @Test
    @DisplayName("getAllowWords: 단어만 필터 적용")
    void getAllowWords_WithWordOnly() {
        // given
        AllowWord aw = new AllowWord(ID, WORD, true);
        given(allowWordRepository.findByWord(WORD))
                .willReturn(Optional.of(aw));

        // when
        List<AllowWordResponseDto> result = service.getAllowWords(null, WORD);

        // then
        assertThat(result)
                .hasSize(1)
                .extracting(AllowWordResponseDto::getWord)
                .containsExactly(WORD);
    }

    @Test
    @DisplayName("getAllowWords: 필터 없음")
    void getAllowWords_NoFilter() {
        // given
        AllowWord aw1 = new AllowWord(1L, "foo", true);
        AllowWord aw2 = new AllowWord(2L, "bar", false);
        given(allowWordRepository.findAll())
                .willReturn(List.of(aw1, aw2));

        // when
        List<AllowWordResponseDto> result = service.getAllowWords(null, null);

        // then
        assertThat(result)
                .hasSize(2)
                .extracting(AllowWordResponseDto::getWord)
                .containsExactlyInAnyOrder("foo", "bar");
    }

    @Test
    @DisplayName("deleteAllowWord: ID 없으면 예외")
    void deleteAllowWord_NotFound() {
        // given
        given(allowWordRepository.findById(ID)).willReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> service.deleteAllowWord(ID))
                .isInstanceOf(AdminException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ALLOW_WORD_NOT_FOUND);
    }

    @Test
    @DisplayName("deleteAllowWord: 정상 삭제 시 Redis 동작 검증")
    void deleteAllowWord_Success() {
        // given: true 일때
        AllowWord awTrue = new AllowWord(ID, WORD, true);
        given(allowWordRepository.findById(ID)).willReturn(Optional.of(awTrue));

        // when / then
        assertThatCode(() -> service.deleteAllowWord(ID))
                .doesNotThrowAnyException();
        then(allowWordRepository).should().delete(awTrue);
        then(eventPublisher).should().publishEvent(any(Object.class));

        // given: false 일때
        reset(allowWordRepository, eventPublisher);
        AllowWord awFalse = new AllowWord(ID, WORD, false);
        given(allowWordRepository.findById(ID)).willReturn(Optional.of(awFalse));

        // when / then
        assertThatCode(() -> service.deleteAllowWord(ID))
                .doesNotThrowAnyException();
        then(allowWordRepository).should().delete(awFalse);
        then(eventPublisher).should(never()).publishEvent(any(Object.class));
    }

    @Test
    @DisplayName("toggleAllowWordStatus: ID 없으면 예외")
    void toggleAllowWordStatus_NotFound() {
        // given
        given(allowWordRepository.findById(ID)).willReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> service.toggleAllowWordStatus(ID))
                .isInstanceOf(AdminException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ALLOW_WORD_NOT_FOUND);
    }

    @Test
    @DisplayName("toggleAllowWordStatus: false -> true 전환")
    void toggleAllowWordStatus_FalseToTrue() {
        // given
        AllowWord aw = new AllowWord(ID, WORD, false);
        given(allowWordRepository.findById(ID)).willReturn(Optional.of(aw));
        given(allowWordRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        // when
        AllowWordResponseDto dto = service.toggleAllowWordStatus(ID);

        // then
        assertThat(dto.isStatus()).isTrue();
        then(eventPublisher).should().publishEvent(any(Object.class));
    }

    @Test
    @DisplayName("toggleAllowWordStatus: true -> false 전환")
    void toggleAllowWordStatus_TrueToFalse() {
        // given
        AllowWord aw = new AllowWord(ID, WORD, true);
        given(allowWordRepository.findById(ID)).willReturn(Optional.of(aw));
        given(allowWordRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        // when
        AllowWordResponseDto dto = service.toggleAllowWordStatus(ID);

        // then
        assertThat(dto.isStatus()).isFalse();
        then(eventPublisher).should().publishEvent(any(Object.class));
    }

    @Test
    @DisplayName("toggleAllowWordStatus: Redis 추가 중 예외 발생")
    void toggleAllowWordStatus_RedisAddThrows() {
        // given
        AllowWord aw = new AllowWord(ID, WORD, false);
        given(allowWordRepository.findById(ID)).willReturn(Optional.of(aw));
        given(allowWordRepository.save(any())).willAnswer(inv -> {
            AllowWord u = inv.getArgument(0);
            u.setStatus(true);
            return u;
        });
        willThrow(new RuntimeException("redis error"))
                .given(eventPublisher).publishEvent(any(Object.class));

        // when / then
        assertThatThrownBy(() -> service.toggleAllowWordStatus(ID))
                .isInstanceOf(AdminException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ALLOW_WORD_UPDATE_FAILED);
    }

    @Test
    @DisplayName("toggleAllowWordStatus: Redis 제거 중 예외 발생")
    void toggleAllowWordStatus_RedisRemoveThrows() {
        // given
        AllowWord aw = new AllowWord(ID, WORD, true);
        given(allowWordRepository.findById(ID)).willReturn(Optional.of(aw));
        given(allowWordRepository.save(any())).willAnswer(inv -> {
            AllowWord u = inv.getArgument(0);
            u.setStatus(false);
            return u;
        });
        willThrow(new RuntimeException("redis error"))
                .given(eventPublisher).publishEvent(any(Object.class));

        // when / then
        assertThatThrownBy(() -> service.toggleAllowWordStatus(ID))
                .isInstanceOf(AdminException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ALLOW_WORD_UPDATE_FAILED);
    }
}
