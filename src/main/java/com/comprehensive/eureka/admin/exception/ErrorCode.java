package com.comprehensive.eureka.admin.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorCode {
    USER_NOT_FOUND(50000, "USER_NOT_FOUND", "해당 사용자를 찾을 수 없습니다.");

    private final int code;
    private final String name;
    private final String message;
}