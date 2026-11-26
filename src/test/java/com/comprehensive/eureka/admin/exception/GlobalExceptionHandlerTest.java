package com.comprehensive.eureka.admin.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void 락_획득_실패와_사용자_상태_변경_실패는_재시도_가능한_503으로_응답한다() {
        assertThat(handler.handleAdminException(new AdminException(ErrorCode.USER_FORBIDDEN_WORDS_CHAT_LOCK_FAILED)).getStatusCode())
                .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(handler.handleAdminException(new AdminException(ErrorCode.USER_STATUS_UPDATE_FAILED)).getStatusCode())
                .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    void 요청_자체의_문제는_400으로_응답한다() {
        assertThat(handler.handleAdminException(new AdminException(ErrorCode.FORBIDDEN_WORD_ALREADY_EXISTS)).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
