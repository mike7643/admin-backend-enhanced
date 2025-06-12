package com.comprehensive.eureka.admin.constant;

public class DomainConstant {
    // 8082
    public static final String CHATBOT_SERVICE_KEY = "chatbot";
    // 8083
    public static final String RECOMMEND_SERVICE_KEY = "recommend";
    // 8084
    public static final String PLAN_SERVICE_KEY = "plan";
    // 8085
    public static final String USER_SERVICE_KEY = "user";
    // 8086
    public static final String ADMIN_SERVICE_KEY = "admin";
    // 8087
    public static final String CHAT_SERVICE_KEY = "chat";
    // 챗봇 모듈 도메인
    public static final String CHATBOT_DOMAIN = "http://localhost:8082";
    // 추천 시스템 모듈 도메인
    public static final String RECOMMEND_DOMAIN = "http://localhost:8083";
    // 요금제 모듈 도메인
    public static final String PLAN_DOMAIN = "http://localhost:8084";
    // 사용자 모듈 도메인
    public static final String USER_DOMAIN = "http://localhost:8085";
    // 관리자 모듈 도메인
    public static final String ADMIN_DOMAIN = "http://localhost:8086";
    // 채팅 모듈 도메인
    public static final String CHAT_DOMAIN = "http://localhost:8087";
    // 슬래쉬
    public static final String SLASH = "/";
    // 화이트리스트 (인증 없이 접근 허용할 경로)
    public static final String[] whiteList = {
            "/auth/healthCheck",
            "/auth/login",
            "/auth/signup",
            "/auth/verify-email"
    };
    // 블랙리스트 (인증 필요, 추가적인 제한 걸 경로 – 예: 관리자 등급)
    public static final String[] blackList = {
            "/**"
    };
    // 리프레쉬 토큰 재발급 경로
    public static final String REFRESH_PATH = "/auth/reissue";
}
