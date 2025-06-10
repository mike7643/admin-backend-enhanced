package com.comprehensive.eureka.admin.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.comprehensive.eureka.admin.dto.BaseResponseDto;
import com.comprehensive.eureka.admin.dto.response.ErrorResponseDto;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * AdminException 처리
     * ErrorCode 에 담긴 코드·메시지를 BaseResponseDto.fail() 로 감싸서 반환
     */
    @ExceptionHandler(AdminException.class)
    public ResponseEntity<BaseResponseDto<ErrorResponseDto>> handleAdminException(AdminException ex) {
        ErrorCode ec = ex.getErrorCode();
        return ResponseEntity
                .badRequest()
                .body(BaseResponseDto.fail(ec));
    }

    /**
     * 그 외 예기치 못한 예외 처리
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<BaseResponseDto<ErrorResponseDto>> handleException(Exception ex) {
        // 필요하다면 별도 INTERNAL_SERVER_ERROR 코드 정의 후 사용
        ErrorCode ec = ErrorCode.INTERNAL_SERVER_ERROR;
        return ResponseEntity
                .status(500)
                .body(BaseResponseDto.fail(ec));
    }
}
