package com.comprehensive.eureka.admin.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    INTERNAL_SERVER_ERROR(60000, "INTERNAL_SERVER_ERROR", "서버 내부 오류가 발생했습니다."),

    // 금칙어 관리 에러 (60010~60019)
    FORBIDDEN_WORD_NOT_FOUND(60010, "FORBIDDEN_WORD_NOT_FOUND", "해당 금칙어를 찾을 수 없습니다."),
    FORBIDDEN_WORD_ALREADY_EXISTS(60011, "FORBIDDEN_WORD_ALREADY_EXISTS", "이미 등록된 금칙어입니다."),
    FORBIDDEN_WORD_CREATE_FAILED(60012, "FORBIDDEN_WORD_CREATE_FAILED", "금칙어 등록에 실패했습니다."),
    FORBIDDEN_WORD_UPDATE_FAILED(60013, "FORBIDDEN_WORD_UPDATE_FAILED", "금칙어 수정에 실패했습니다."),
    FORBIDDEN_WORD_DELETE_FAILED(60014, "FORBIDDEN_WORD_DELETE_FAILED", "금칙어 삭제에 실패했습니다."),
    FORBIDDEN_WORD_TOGGLE_FAILED(60015, "FORBIDDEN_WORD_TOGGLE_FAILED", "금칙어 사용 여부 전환에 실패했습니다."),
    FORBIDDEN_WORD_CHATBOT_ADD_FAILED(60016, "FORBIDDEN_WORD_CHATBOT_ADD_FAILED", "챗봇 모듈에 금칙어 추가 요청에 실패했습니다."),
    FORBIDDEN_WORD_CHATBOT_DELETE_FAILED(60017, "FORBIDDEN_WORD_CHATBOT_DELETE_FAILED", "챗봇 모듈에 금칙어 삭제 요청에 실패했습니다."),

    // 금칙어 로그 관리 에러 (60020~60029)
    FORBIDDEN_WORD_LOG_NOT_FOUND(60020, "FORBIDDEN_WORD_LOG_NOT_FOUND", "해당 금칙어 로그를 찾을 수 없습니다."),
    FORBIDDEN_WORD_LOG_RETRIEVE_FAILED(60021, "FORBIDDEN_WORD_LOG_RETRIEVE_FAILED", "금칙어 로그 조회에 실패했습니다."),
    FORBIDDEN_WORD_LOG_DELETE_FAILED(60022, "FORBIDDEN_WORD_LOG_DELETE_FAILED", "금칙어 로그 삭제에 실패했습니다."),

    // 사용자_금칙어_채팅 기록 조회 에러 (60030~60039)
    USER_FORBIDDEN_WORDS_CHAT_RETRIEVE_FAILED(60031, "USER_FORBIDDEN_WORDS_CHAT_RETRIEVE_FAILED", "사용자 금칙어 채팅 기록 조회에 실패했습니다."),

    // 사용자_금칙어_채팅 기록 삭제 및 집계 에러 (60040~60049)
    USER_FORBIDDEN_WORDS_CHAT_DELETE_FAILED(60040, "USER_FORBIDDEN_WORDS_CHAT_DELETE_FAILED", "사용자 금칙어 채팅 기록 삭제에 실패했습니다."),
    USER_FORBIDDEN_WORDS_CHAT_AGGREGATE_FAILED(60041, "USER_FORBIDDEN_WORDS_CHAT_AGGREGATE_FAILED", "삭제 후 집계 호출에 실패했습니다."),
    USER_FORBIDDEN_WORDS_CHAT_SAVE_FAILED(60042, "USER_FORBIDDEN_WORDS_CHAT_SAVE_FAILED", "사용자 금칙어 채팅 기록 저장에 실패했습니다."),
    CHAT_MESSAGE_RETRIEVE_FAILED(60043, "CHAT_MESSAGE_RETRIEVE_FAILED", "채팅 메시지 조회에 실패했습니다."),

    // 사용자 상태 업데이트 에러 (60050~60059)
    USER_STATUS_UPDATE_FAILED(60050, "USER_STATUS_UPDATE_FAILED", "사용자 상태 업데이트에 실패했습니다."),

    // 허용어 관리 에러 (60060~60069)
    ALLOW_WORD_NOT_FOUND(60060, "ALLOW_WORD_NOT_FOUND", "해당 허용어를 찾을 수 없습니다."),
    ALLOW_WORD_ALREADY_EXISTS(60061, "ALLOW_WORD_ALREADY_EXISTS", "이미 등록된 허용어입니다."),
    ALLOW_WORD_CREATE_FAILED(60062, "ALLOW_WORD_CREATE_FAILED", "허용어 등록에 실패했습니다.");


    private final int code;
    private final String name;
    private final String message;
}
