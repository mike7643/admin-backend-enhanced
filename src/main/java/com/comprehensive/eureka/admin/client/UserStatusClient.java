package com.comprehensive.eureka.admin.client;

import com.comprehensive.eureka.admin.dto.request.UpdateUserStatusRequestDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

/**
 * 사용자 모듈의 상태 변경 API 호출을 담당한다.
 * 이 호출은 사용자 단위 분산락을 잡은 상태에서 실행되므로, 락 임대 시간(10초)보다 짧은 타임아웃을 둔다.
 */
@Component
public class UserStatusClient {

    private static final Duration TIMEOUT = Duration.ofSeconds(3);

    private final WebClient webClient;
    private final String userServiceUrl;

    public UserStatusClient(WebClient webClient,
                            @Value("${admin.user-service.url:http://localhost:8085}") String userServiceUrl) {
        this.webClient = webClient;
        this.userServiceUrl = userServiceUrl;
    }

    public void updateStatus(UpdateUserStatusRequestDto request) {
        webClient.put()
                .uri(userServiceUrl + "/user/status")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Void.class)
                .block(TIMEOUT);
    }
}
