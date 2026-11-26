package com.comprehensive.eureka.admin.exception;

import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.comprehensive.eureka.admin.dto.BaseResponseDto;
import com.comprehensive.eureka.admin.dto.ErrorResponseDto;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 잠시 후 같은 요청을 다시 보내면 성공할 수 있는 실패.
     * 위반 기록 저장은 eventId로 멱등하게 처리되므로, 호출 측(챗봇)이 5xx를 받고 재시도해도 중복 집계되지 않는다.
     */
    private static final Set<ErrorCode> RETRYABLE_ERRORS = Set.of(
            ErrorCode.USER_FORBIDDEN_WORDS_CHAT_LOCK_FAILED,
            ErrorCode.USER_STATUS_UPDATE_FAILED
    );

    @ExceptionHandler(AdminException.class)
    public ResponseEntity<BaseResponseDto<ErrorResponseDto>> handleAdminException(AdminException ex) {
        ErrorCode ec = ex.getErrorCode();
        HttpStatus status = RETRYABLE_ERRORS.contains(ec) ? HttpStatus.SERVICE_UNAVAILABLE : HttpStatus.BAD_REQUEST;
        return ResponseEntity
                .status(status)
                .body(BaseResponseDto.fail(ec));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<BaseResponseDto<ErrorResponseDto>> handleException(Exception ex) {
        ErrorCode ec = ErrorCode.INTERNAL_SERVER_ERROR;
        return ResponseEntity
                .status(500)
                .body(BaseResponseDto.fail(ec));
    }
}
